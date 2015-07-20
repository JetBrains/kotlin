/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import com.intellij.refactoring.util.RefactoringUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isUnit
import java.util.ArrayList
import java.util.Comparator
import java.util.LinkedHashMap
import java.util.LinkedHashSet

class KotlinPullUpHelper(
        private val javaData: PullUpData,
        private val data: KotlinPullUpData
) : PullUpHelper<MemberInfoBase<PsiMember>> {
    companion object {
        private var JetElement.newFqName: FqName? by CopyableUserDataProperty(Key.create("NEW_FQ_NAME"))
        private var JetElement.replaceWithTargetThis: Boolean? by CopyableUserDataProperty(Key.create("REPLACE_WITH_TARGET_THIS"))
        private var JetElement.newTypeText: String? by CopyableUserDataProperty(Key.create("NEW_TYPE_TEXT"))
    }

    private fun JetExpression.isMovable(): Boolean {
        return accept(
                object: JetVisitor<Boolean, Nothing?>() {
                    override fun visitJetElement(element: JetElement, arg: Nothing?): Boolean {
                        return element.allChildren.all { (it as? JetElement)?.accept(this, arg) ?: true }
                    }

                    override fun visitJetFile(file: JetFile, data: Nothing?) = false

                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression, arg: Nothing?): Boolean {
                        val resolvedCall = expression.getResolvedCall(data.resolutionFacade.analyze(expression)) ?: return true
                        val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.getExpression()
                        if (receiver != null && receiver !is JetThisExpression && receiver !is JetSuperExpression) return true

                        var descriptor: DeclarationDescriptor = resolvedCall.getResultingDescriptor()
                        if (descriptor is ConstructorDescriptor) {
                            descriptor = descriptor.getContainingDeclaration()
                        }
                        // todo: local functions
                        if (descriptor is ValueParameterDescriptor) return true
                        if (descriptor is ClassDescriptor && !descriptor.isInner()) return true
                        if (descriptor is MemberDescriptor) {
                            if (descriptor.getSource().getPsi() in propertiesToMoveInitializers) return true
                            descriptor = descriptor.getContainingDeclaration()
                        }
                        return descriptor is PackageFragmentDescriptor
                               || (descriptor is ClassDescriptor && DescriptorUtils.isSubclass(data.targetClassDescriptor, descriptor))
                    }
                },
                null
        )
    }

    private fun getCommonInitializer(
            currentInitializer: JetExpression?,
            scope: JetBlockExpression?,
            propertyDescriptor: PropertyDescriptor,
            elementsToRemove: MutableSet<JetElement>): JetExpression? {
        if (scope == null) return currentInitializer

        var initializerCandidate: JetExpression? = null

        for (statement in scope.getStatements()) {
            statement.asAssignment()?.let body@ {
                val lhs = JetPsiUtil.safeDeparenthesize(it.getLeft() ?: return@body)
                val receiver = (lhs as? JetQualifiedExpression)?.getReceiverExpression()
                if (receiver != null && receiver !is JetThisExpression) return@body

                val resolvedCall = lhs.getResolvedCall(data.resolutionFacade.analyze(it)) ?: return@body
                if (resolvedCall.getResultingDescriptor() != propertyDescriptor) return@body

                if (initializerCandidate == null) {
                    if (currentInitializer == null) {
                        if (!statement.isMovable()) return null

                        initializerCandidate = statement
                        elementsToRemove.add(statement)
                    }
                    else {
                        if (!JetPsiUnifier.DEFAULT.unify(statement, currentInitializer).matched) return null

                        initializerCandidate = currentInitializer
                        elementsToRemove.add(statement)
                    }
                }
                else if (!JetPsiUnifier.DEFAULT.unify(statement, initializerCandidate).matched) return null
            }
        }

        return initializerCandidate
    }

    private data class InitializerInfo(
            val initializer: JetExpression?,
            val usedProperties: Set<JetProperty>,
            val usedParameters: Set<JetParameter>,
            val elementsToRemove: Set<JetElement>
    )

    private fun getInitializerInfo(property: JetProperty,
                                   propertyDescriptor: PropertyDescriptor,
                                   targetConstructor: JetElement): InitializerInfo? {
        val sourceConstructors = targetToSourceConstructors[targetConstructor] ?: return null
        val elementsToRemove = LinkedHashSet<JetElement>()
        val commonInitializer = sourceConstructors.fold(null as JetExpression?) { commonInitializer, constructor ->
            val body = (constructor as? JetSecondaryConstructor)?.getBodyExpression()
            getCommonInitializer(commonInitializer, body, propertyDescriptor, elementsToRemove)
        }
        if (commonInitializer == null) {
            elementsToRemove.clear()
        }

        val usedProperties = LinkedHashSet<JetProperty>()
        val usedParameters = LinkedHashSet<JetParameter>()
        val visitor = object : JetTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val context = data.resolutionFacade.analyze(expression)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.getExpression()
                if (receiver != null && receiver !is JetThisExpression) return
                val target = (resolvedCall.getResultingDescriptor() as? DeclarationDescriptorWithSource)?.getSource()?.getPsi()
                when (target) {
                    is JetParameter -> usedParameters.add(target)
                    is JetProperty -> usedProperties.add(target)
                }
            }
        }
        commonInitializer?.accept(visitor)
        if (targetConstructor == data.targetClass.getPrimaryConstructor() ?: data.targetClass) {
            property.getInitializer()?.accept(visitor)
        }

        return InitializerInfo(commonInitializer, usedProperties, usedParameters, elementsToRemove)
    }

    private val propertiesToMoveInitializers = with(data) {
        membersToMove
                .filterIsInstance<JetProperty>()
                .filter {
                    val descriptor = memberDescriptors[it] as? PropertyDescriptor
                    descriptor != null && data.sourceClassContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] ?: false
                }
    }

    private val targetToSourceConstructors = LinkedHashMap<JetElement, MutableList<JetElement>>().let { result ->
        if (!data.targetClass.isInterface()) {
            result[data.targetClass.getPrimaryConstructor() ?: data.targetClass] = ArrayList<JetElement>()
            data.sourceClass.accept(
                    object : JetTreeVisitorVoid() {
                        private fun processConstructorReference(expression: JetReferenceExpression, callingConstructorElement: JetElement) {
                            val descriptor = data.resolutionFacade.analyze(expression)[BindingContext.REFERENCE_TARGET, expression]
                            val constructorElement = (descriptor as? DeclarationDescriptorWithSource)?.getSource()?.getPsi() ?: return
                            if (constructorElement == data.targetClass
                                || (constructorElement as? JetConstructor<*>)?.getContainingClassOrObject() == data.targetClass) {
                                result.getOrPut(constructorElement as JetElement, { ArrayList() }).add(callingConstructorElement)
                            }
                        }

                        override fun visitDelegationToSuperCallSpecifier(specifier: JetDelegatorToSuperCall) {
                            val constructorRef = specifier.getCalleeExpression().getConstructorReferenceExpression() ?: return
                            val containingClass = specifier.getStrictParentOfType<JetClassOrObject>() ?: return
                            val callingConstructorElement = containingClass.getPrimaryConstructor() ?: containingClass
                            processConstructorReference(constructorRef, callingConstructorElement)
                        }

                        override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
                            val constructorRef = constructor.getDelegationCall().getCalleeExpression() ?: return
                            processConstructorReference(constructorRef, constructor)
                        }
                    }
            )
        }
        result
    }

    private val targetConstructorToPropertyInitializerInfoMap = LinkedHashMap<JetElement, Map<JetProperty, InitializerInfo>>().let { result ->
        for (targetConstructor in targetToSourceConstructors.keySet()) {
            val propertyToInitializerInfo = LinkedHashMap<JetProperty, InitializerInfo>()
            for (property in propertiesToMoveInitializers) {
                val propertyDescriptor = data.memberDescriptors[property] as? PropertyDescriptor ?: continue
                propertyToInitializerInfo[property] = getInitializerInfo(property, propertyDescriptor, targetConstructor) ?: continue
            }
            val unmovableProperties = RefactoringUtil.transitiveClosure(
                    object : RefactoringUtil.Graph<JetProperty> {
                        override fun getVertices() = propertyToInitializerInfo.keySet()

                        override fun getTargets(source: JetProperty) = propertyToInitializerInfo[source]?.usedProperties
                    },
                    { !propertyToInitializerInfo.containsKey(it) }
            )

            propertyToInitializerInfo.keySet().removeAll(unmovableProperties)
            result[targetConstructor] = propertyToInitializerInfo
        }
        result
    }

    private var dummyField: PsiField? = null

    private fun addMovedMember(newMember: JetNamedDeclaration) {
        if (newMember is JetProperty) {
            // Add dummy light field since PullUpProcessor won't invoke moveFieldInitializations() if no PsiFields are present
            if (dummyField == null) {
                val factory = JavaPsiFacade.getElementFactory(newMember.getProject())
                val dummyField = object: LightField(
                        newMember.getManager(),
                        factory.createField("dummy", PsiType.BOOLEAN),
                        factory.createClass("Dummy")
                ) {
                    // Prevent processing by JavaPullUpHelper
                    override fun getLanguage() = JetLanguage.INSTANCE
                }
                javaData.getMovedMembers().add(dummyField)
            }
        }

        when (newMember) {
            is JetProperty, is JetNamedFunction -> {
                newMember.getRepresentativeLightMethod()?.let { javaData.getMovedMembers().add(it) }
            }
            is JetClassOrObject -> {
                newMember.toLightClass()?.let { javaData.getMovedMembers().add(it) }
            }
        }
    }

    private fun willBeUsedInSourceClass(member: PsiElement): Boolean {
        return !ReferencesSearch
                .search(member, LocalSearchScope(data.sourceClass), false)
                .all { it.getElement().parentsWithSelf.any { it in data.membersToMove } }
    }

    private fun liftToProtected(declaration: JetNamedDeclaration, ignoreUsages: Boolean = false) {
        if (!declaration.hasModifier(JetTokens.PRIVATE_KEYWORD)) return
        if (ignoreUsages || willBeUsedInSourceClass(declaration)) declaration.addModifierWithSpace(JetTokens.PROTECTED_KEYWORD)
    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {
        val member = info.getMember().namedUnwrappedElement as? JetNamedDeclaration ?: return

        if (data.targetClass.isInterface()) {
            member.removeModifier(JetTokens.PUBLIC_KEYWORD)
            return
        }

        if (member.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
            member.accept(
                    object: JetVisitorVoid() {
                        override fun visitNamedDeclaration(declaration: JetNamedDeclaration) {
                            when (declaration) {
                                is JetClass -> {
                                    liftToProtected(declaration)
                                    declaration.getDeclarations().forEach { it.accept(this) }
                                }
                                is JetNamedFunction, is JetProperty -> {
                                    liftToProtected(declaration, declaration == member && info.isToAbstract())
                                }
                            }
                        }
                    }
            )
        }
    }

    override fun encodeContextInfo(info: MemberInfoBase<PsiMember>) {

    }

    private fun fixOverrideAndGetClashingSuper(sourceMember: JetCallableDeclaration,
                                               targetMember: JetCallableDeclaration): JetCallableDeclaration? {
        val memberDescriptor = data.memberDescriptors[sourceMember] as CallableMemberDescriptor

        if (memberDescriptor.getOverriddenDescriptors().isEmpty()) {
            targetMember.removeOverrideModifier()
            return null
        }

        val clashingSuperDescriptor = data.getClashingMemberInTargetClass(memberDescriptor) ?: return null
        if (clashingSuperDescriptor.getOverriddenDescriptors().isEmpty()) {
            targetMember.removeOverrideModifier()
        }
        return clashingSuperDescriptor.getSource().getPsi() as? JetCallableDeclaration
    }

    private fun makeAbstract(sourceMember: JetCallableDeclaration, targetMember: JetCallableDeclaration) {
        if (!data.targetClass.isInterface()) {
            targetMember.addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
        }

        if (sourceMember.getTypeReference() == null) {
            var type = (data.memberDescriptors[sourceMember] as CallableMemberDescriptor).getReturnType()
            if (type == null || type.isError()) {
                type = KotlinBuiltIns.getInstance().getNullableAnyType()
            }
            else {
                type = data.sourceToTargetClassSubstitutor.substitute(type, Variance.INVARIANT)
                       ?: KotlinBuiltIns.getInstance().getNullableAnyType()
            }

            if (sourceMember is JetProperty || !type.isUnit()) {
                val typeRef = JetPsiFactory(sourceMember).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
                targetMember.setTypeReference(typeRef)
            }
        }

        val deleteFrom = when (sourceMember) {
            is JetProperty -> {
                targetMember as JetProperty
                val accessors = targetMember.getAccessors()
                targetMember.getEqualsToken() ?: targetMember.getDelegate() ?: accessors.firstOrNull()
            }

            is JetNamedFunction -> {
                targetMember as JetNamedFunction
                targetMember.getEqualsToken() ?: targetMember.getBodyExpression()
            }

            else -> null
        }

        if (deleteFrom != null) {
            targetMember.deleteChildRange(deleteFrom, targetMember.getLastChild())
        }
    }

    private fun addMemberToTarget(targetMember: JetNamedDeclaration): JetNamedDeclaration {
        val anchor = data.targetClass.getDeclarations().filterIsInstance(targetMember.javaClass).lastOrNull()
        val movedMember = when {
            anchor == null && targetMember is JetProperty -> data.targetClass.addDeclarationBefore(targetMember, null)
            else -> data.targetClass.addDeclarationAfter(targetMember, anchor)
        }
        return movedMember as JetNamedDeclaration
    }

    private fun doAddCallableMember(memberCopy: JetCallableDeclaration, clashingSuper: JetCallableDeclaration?): JetCallableDeclaration {
        if (clashingSuper != null && clashingSuper.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            return clashingSuper.replaced(memberCopy)
        }
        return addMemberToTarget(memberCopy) as JetCallableDeclaration
    }

    private fun markElements(member: JetNamedDeclaration): List<JetElement> {
        val affectedElements = ArrayList<JetElement>()

        member.accept(
                object: JetVisitorVoid() {
                    private fun visitSuperOrThis(expression: JetInstanceExpressionWithLabel) {
                        val referenceTarget = data.sourceClassContext[BindingContext.REFERENCE_TARGET, expression.getInstanceReference()]
                        if (referenceTarget == data.targetClassDescriptor) {
                            expression.replaceWithTargetThis = true
                            affectedElements.add(expression)
                        }
                    }

                    override fun visitElement(element: PsiElement) {
                        element.allChildren.forEach { it.accept(this) }
                    }

                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                        val resolvedCall = expression.getResolvedCall(data.sourceClassContext) ?: return
                        var receiver = resolvedCall.getExplicitReceiverValue()
                        if (!receiver.exists()) {
                            receiver = resolvedCall.getExtensionReceiver()
                        }
                        if (!receiver.exists()) {
                            receiver = resolvedCall.getDispatchReceiver()
                        }
                        if (!receiver.exists()) return

                        val implicitThis = receiver.getType().getConstructor().getDeclarationDescriptor() as? ClassDescriptor ?: return
                        if (implicitThis.isCompanionObject()
                            && DescriptorUtils.isAncestor(data.sourceClassDescriptor, implicitThis, true)) {
                            val qualifierFqName = implicitThis.importableFqName ?: return

                            expression.newFqName = FqName("${qualifierFqName.asString()}.${expression.getReferencedName()}")
                            affectedElements.add(expression)
                        }
                    }

                    override fun visitThisExpression(expression: JetThisExpression) {
                        visitSuperOrThis(expression)
                    }

                    override fun visitSuperExpression(expression: JetSuperExpression) {
                        visitSuperOrThis(expression)
                    }

                    override fun visitTypeReference(typeReference: JetTypeReference) {
                        val oldType = data.sourceClassContext[BindingContext.TYPE, typeReference] ?: return
                        val newType = data.sourceToTargetClassSubstitutor.substitute(oldType, Variance.INVARIANT) ?: return
                        typeReference.newTypeText = IdeDescriptorRenderers.SOURCE_CODE.renderType(newType)
                        affectedElements.add(typeReference)
                    }
                }
        )

        return affectedElements
    }

    private fun processMarkedElements(member: JetNamedDeclaration) {
        val psiFactory = JetPsiFactory(member)
        val targetThis = psiFactory.createExpression("this@${data.targetClassDescriptor.getName().asString()}")
        val shorteningOptionsForThis = ShortenReferences.Options(removeThisLabels = true, removeThis = true)

        member.accept(
                object: JetVisitorVoid() {
                    private fun visitSuperOrThis(expression: JetInstanceExpressionWithLabel) {
                        expression.replaceWithTargetThis?.let {
                            expression.replaceWithTargetThis = null

                            val newThisExpression = expression.replace(targetThis) as JetExpression
                            newThisExpression.getQualifiedExpressionForReceiverOrThis().addToShorteningWaitSet(shorteningOptionsForThis)
                        }
                    }

                    override fun visitElement(element: PsiElement) {
                        for (it in element.allChildren.toList()) {
                            it.accept(this)
                        }
                    }

                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                        expression.newFqName?.let {
                            expression.newFqName = null

                            expression.mainReference.bindToFqName(it)
                        }
                    }

                    override fun visitThisExpression(expression: JetThisExpression) {
                        visitSuperOrThis(expression)
                    }

                    override fun visitSuperExpression(expression: JetSuperExpression) {
                        visitSuperOrThis(expression)
                    }

                    override fun visitTypeReference(typeReference: JetTypeReference) {
                        typeReference.newTypeText?.let {
                            typeReference.newTypeText = null

                            (typeReference.replace(psiFactory.createType(it)) as JetElement).addToShorteningWaitSet()
                        }
                    }
                }
        )
    }

    private fun clearMarking(markedElements: List<JetElement>) {
        markedElements.forEach {
            it.newFqName = null
            it.newTypeText = null
            it.replaceWithTargetThis = null
        }
    }

    // TODO: Formatting rules don't apply here for some reason
    private fun JetNamedDeclaration.addModifierWithSpace(modifier: JetModifierKeywordToken) {
        addModifier(modifier)
        addAfter(JetPsiFactory(this).createWhiteSpace(), getModifierList())
    }

    override fun move(info: MemberInfoBase<PsiMember>, substitutor: PsiSubstitutor) {
        val member = info.getMember().namedUnwrappedElement as? JetNamedDeclaration ?: return

        if (member is JetClass && info.getOverrides() != null)  {
            val psiFactory = JetPsiFactory(member)

            val classDescriptor = data.memberDescriptors[member] as? ClassDescriptor ?: return

            val currentSpecifier =
                    data.sourceClass.getDelegationSpecifiers()
                            .filterIsInstance<JetDelegatorToSuperClass>()
                            .firstOrNull {
                                val referencedType = data.sourceClassContext[BindingContext.TYPE, it.getTypeReference()]
                                referencedType?.getConstructor()?.getDeclarationDescriptor() == classDescriptor
                            } ?: return

            data.sourceClass.removeDelegationSpecifier(currentSpecifier)

            if (!DescriptorUtils.isSubclass(data.targetClassDescriptor, classDescriptor)) {
                val referencedType = data.sourceClassContext[BindingContext.TYPE, currentSpecifier.getTypeReference()]!!
                val typeInTargetClass = data.sourceToTargetClassSubstitutor.substitute(referencedType, Variance.INVARIANT)
                if (typeInTargetClass != null && !typeInTargetClass.isError) {
                    val renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(typeInTargetClass)
                    data.targetClass.addDelegationSpecifier(psiFactory.createDelegatorToSuperClass(renderedType)).addToShorteningWaitSet()
                }
            }


            return
        }

        val markedElements = markElements(member)
        val memberCopy = member.copy() as JetNamedDeclaration

        try {
            var movedMember: JetNamedDeclaration
            when (member) {
                is JetCallableDeclaration -> {
                    val clashingSuper = fixOverrideAndGetClashingSuper(member, memberCopy as JetCallableDeclaration)

                    val originalIsAbstract = member.hasModifier(JetTokens.ABSTRACT_KEYWORD)
                    val toAbstract = when {
                        info.isToAbstract() -> true
                        !data.targetClass.isInterface() -> false
                        member is JetProperty -> member.mustBeAbstractInInterface()
                        else -> false
                    }
                    if (toAbstract) {
                        if (!originalIsAbstract) {
                            makeAbstract(member, memberCopy)
                        }

                        movedMember = doAddCallableMember(memberCopy, clashingSuper)
                        if (member.getTypeReference() == null) {
                            movedMember.getTypeReference()?.addToShorteningWaitSet()
                        }
                        if (originalIsAbstract) {
                            member.delete()
                        }
                        else if (!member.hasModifier(JetTokens.OVERRIDE_KEYWORD)) {
                            member.addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
                        }
                    }
                    else {
                        movedMember = doAddCallableMember(memberCopy, clashingSuper)
                        member.delete()
                    }

                    if (originalIsAbstract && data.targetClass.isInterface()) {
                        movedMember.removeModifier(JetTokens.ABSTRACT_KEYWORD)
                    }

                    if (!data.targetClass.isInterface()
                        && !data.targetClass.hasModifier(JetTokens.ABSTRACT_KEYWORD)
                        && (movedMember.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
                        data.targetClass.addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
                    }
                }

                is JetClassOrObject -> {
                    if (data.targetClass.isInterface()) {
                        memberCopy.removeModifier(JetTokens.INNER_KEYWORD)
                    }
                    movedMember = addMemberToTarget(memberCopy) as JetClassOrObject
                    member.delete()
                }

                else -> return
            }

            processMarkedElements(movedMember)

            addMovedMember(movedMember)
        }
        finally {
            clearMarking(markedElements)
        }
    }

    override fun postProcessMember(member: PsiMember) {

    }

    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>) {
        val psiFactory = JetPsiFactory(data.sourceClass)

        fun JetClassOrObject.getOrCreateClassInitializer(): JetClassInitializer {
            getOrCreateBody().getDeclarations().lastOrNull { it is JetClassInitializer }?.let { return it as JetClassInitializer }
            return addDeclaration(psiFactory.createAnonymousInitializer()) as JetClassInitializer
        }

        fun JetElement.getConstructorBodyBlock(): JetBlockExpression? {
            return when (this) {
                is JetClassOrObject -> {
                    getOrCreateClassInitializer().getBody()
                }
                is JetPrimaryConstructor -> {
                    getContainingClassOrObject().getOrCreateClassInitializer().getBody()
                }
                is JetSecondaryConstructor -> {
                    getBodyExpression() ?: add(psiFactory.createEmptyBody())
                }
                else -> null
            } as? JetBlockExpression
        }

        fun JetClassOrObject.getDelegatorToSuperCall(): JetDelegatorToSuperCall? {
            return getDelegationSpecifiers().singleOrNull { it is JetDelegatorToSuperCall } as? JetDelegatorToSuperCall
        }

        fun addUsedParameters(constructorElement: JetElement, info: InitializerInfo) {
            if (!info.usedParameters.isNotEmpty()) return
            val constructor: JetConstructor<*> = when (constructorElement) {
                is JetConstructor<*> -> constructorElement
                is JetClass -> constructorElement.createPrimaryConstructorIfAbsent()
                else -> return
            }

            with(constructor.getValueParameterList()!!) {
                info.usedParameters.forEach { addParameter(it) }
            }
            targetToSourceConstructors[constructorElement]!!.forEach {
                val superCall: JetCallElement? = when (it) {
                    is JetClassOrObject -> it.getDelegatorToSuperCall()
                    is JetPrimaryConstructor -> it.getContainingClassOrObject().getDelegatorToSuperCall()
                    is JetSecondaryConstructor -> {
                        if (it.hasImplicitDelegationCall()) {
                            it.replaceImplicitDelegationCallWithExplicit(false)
                        }
                        else {
                            it.getDelegationCall()
                        }
                    }
                    else -> null
                }
                superCall?.getValueArgumentList()?.let { args ->
                    info.usedParameters.forEach {
                        args.addArgument(psiFactory.createArgument(psiFactory.createExpression(it.getName() ?: "_")))
                    }
                }
            }
        }

        for ((constructorElement, propertyToInitializerInfo) in targetConstructorToPropertyInitializerInfoMap.entrySet()) {
            val properties = propertyToInitializerInfo.keySet().toArrayList().sortBy(
                    object : Comparator<JetProperty> {
                        override fun compare(property1: JetProperty, property2: JetProperty): Int {
                            val info1 = propertyToInitializerInfo[property1]!!
                            val info2 = propertyToInitializerInfo[property2]!!
                            return when {
                                property2 in info1.usedProperties -> -1
                                property1 in info2.usedProperties -> 1
                                else -> 0
                            }
                        }
                    }
            )

            for (oldProperty in properties) {
                val info = propertyToInitializerInfo[oldProperty]!!

                addUsedParameters(constructorElement, info)

                info.initializer?.let {
                    val body = constructorElement.getConstructorBodyBlock()
                    body?.addAfter(it, body.getStatements().lastOrNull() ?: body.getLBrace()!!)
                }
                info.elementsToRemove.forEach { it.delete() }
            }
        }
    }

    override fun updateUsage(element: PsiElement) {

    }
}
