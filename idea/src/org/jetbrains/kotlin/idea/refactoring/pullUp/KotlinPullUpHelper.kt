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

import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import com.intellij.refactoring.util.RefactoringUtil
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.refactoring.createJavaField
import org.jetbrains.kotlin.idea.core.refactoring.createJavaMethod
import org.jetbrains.kotlin.idea.core.refactoring.createPrimaryConstructorIfAbsent
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.util.anonymousObjectSuperTypeOrNull
import org.jetbrains.kotlin.idea.util.psi.patternMatching.JetPsiUnifier
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.asAssignment
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.resolvedCallUtil.getExplicitReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.Variance
import java.util.*

class KotlinPullUpHelper(
        private val javaData: PullUpData,
        private val data: KotlinPullUpData
) : PullUpHelper<MemberInfoBase<PsiMember>> {
    private fun JetExpression.isMovable(): Boolean {
        return accept(
                object: JetVisitor<Boolean, Nothing?>() {
                    override fun visitJetElement(element: JetElement, arg: Nothing?): Boolean {
                        return element.allChildren.all { (it as? JetElement)?.accept(this, arg) ?: true }
                    }

                    override fun visitJetFile(file: JetFile, data: Nothing?) = false

                    override fun visitSimpleNameExpression(expression: JetSimpleNameExpression, arg: Nothing?): Boolean {
                        val resolvedCall = expression.getResolvedCall(data.resolutionFacade.analyze(expression)) ?: return true
                        val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.expression
                        if (receiver != null && receiver !is JetThisExpression && receiver !is JetSuperExpression) return true

                        var descriptor: DeclarationDescriptor = resolvedCall.resultingDescriptor
                        if (descriptor is ConstructorDescriptor) {
                            descriptor = descriptor.containingDeclaration
                        }
                        // todo: local functions
                        if (descriptor is ValueParameterDescriptor) return true
                        if (descriptor is ClassDescriptor && !descriptor.isInner) return true
                        if (descriptor is MemberDescriptor) {
                            if (descriptor.source.getPsi() in propertiesToMoveInitializers) return true
                            descriptor = descriptor.containingDeclaration
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

        for (statement in scope.statements) {
            statement.asAssignment()?.let body@ {
                val lhs = JetPsiUtil.safeDeparenthesize(it.left ?: return@body)
                val receiver = (lhs as? JetQualifiedExpression)?.receiverExpression
                if (receiver != null && receiver !is JetThisExpression) return@body

                val resolvedCall = lhs.getResolvedCall(data.resolutionFacade.analyze(it)) ?: return@body
                if (resolvedCall.resultingDescriptor != propertyDescriptor) return@body

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
            val body = (constructor as? JetSecondaryConstructor)?.bodyExpression
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
                val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.expression
                if (receiver != null && receiver !is JetThisExpression) return
                val target = (resolvedCall.resultingDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
                when (target) {
                    is JetParameter -> usedParameters.add(target)
                    is JetProperty -> usedProperties.add(target)
                }
            }
        }
        commonInitializer?.accept(visitor)
        if (targetConstructor == (data.targetClass as? JetClass)?.getPrimaryConstructor() ?: data.targetClass) {
            property.initializer?.accept(visitor)
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
        if (!data.isInterfaceTarget && data.targetClass is JetClass) {
            result[data.targetClass.getPrimaryConstructor() ?: data.targetClass] = ArrayList<JetElement>()
            data.sourceClass.accept(
                    object : JetTreeVisitorVoid() {
                        private fun processConstructorReference(expression: JetReferenceExpression, callingConstructorElement: JetElement) {
                            val descriptor = data.resolutionFacade.analyze(expression)[BindingContext.REFERENCE_TARGET, expression]
                            val constructorElement = (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() ?: return
                            if (constructorElement == data.targetClass
                                || (constructorElement as? JetConstructor<*>)?.getContainingClassOrObject() == data.targetClass) {
                                result.getOrPut(constructorElement as JetElement, { ArrayList() }).add(callingConstructorElement)
                            }
                        }

                        override fun visitDelegationToSuperCallSpecifier(specifier: JetDelegatorToSuperCall) {
                            val constructorRef = specifier.calleeExpression.constructorReferenceExpression ?: return
                            val containingClass = specifier.getStrictParentOfType<JetClassOrObject>() ?: return
                            val callingConstructorElement = containingClass.getPrimaryConstructor() ?: containingClass
                            processConstructorReference(constructorRef, callingConstructorElement)
                        }

                        override fun visitSecondaryConstructor(constructor: JetSecondaryConstructor) {
                            val constructorRef = constructor.getDelegationCall().calleeExpression ?: return
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
                val factory = JavaPsiFacade.getElementFactory(newMember.project)
                val dummyField = object: LightField(
                        newMember.manager,
                        factory.createField("dummy", PsiType.BOOLEAN),
                        factory.createClass("Dummy")
                ) {
                    // Prevent processing by JavaPullUpHelper
                    override fun getLanguage() = JetLanguage.INSTANCE
                }
                javaData.movedMembers.add(dummyField)
            }
        }

        when (newMember) {
            is JetProperty, is JetNamedFunction -> {
                newMember.getRepresentativeLightMethod()?.let { javaData.movedMembers.add(it) }
            }
            is JetClassOrObject -> {
                newMember.toLightClass()?.let { javaData.movedMembers.add(it) }
            }
        }
    }

    private fun willBeUsedInSourceClass(member: PsiElement): Boolean {
        return !ReferencesSearch
                .search(member, LocalSearchScope(data.sourceClass), false)
                .all { it.element.parentsWithSelf.any { it in data.membersToMove } }
    }

    private fun liftToProtected(declaration: JetNamedDeclaration, ignoreUsages: Boolean = false) {
        if (!declaration.hasModifier(JetTokens.PRIVATE_KEYWORD)) return
        if (ignoreUsages || willBeUsedInSourceClass(declaration)) declaration.addModifierWithSpace(JetTokens.PROTECTED_KEYWORD)
    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {
        val member = info.member.namedUnwrappedElement as? JetNamedDeclaration ?: return

        if (data.isInterfaceTarget) {
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
                                    declaration.declarations.forEach { it.accept(this) }
                                }
                                is JetNamedFunction, is JetProperty -> {
                                    liftToProtected(declaration, declaration == member && info.isToAbstract)
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

        if (memberDescriptor.overriddenDescriptors.isEmpty()) {
            targetMember.removeOverrideModifier()
            return null
        }

        val clashingSuperDescriptor = data.getClashingMemberInTargetClass(memberDescriptor) ?: return null
        if (clashingSuperDescriptor.overriddenDescriptors.isEmpty()) {
            targetMember.removeOverrideModifier()
        }
        return clashingSuperDescriptor.source.getPsi() as? JetCallableDeclaration
    }

    private fun moveSuperInterface(member: JetClass, substitutor: PsiSubstitutor) {
        val classDescriptor = data.memberDescriptors[member] as? ClassDescriptor ?: return
        val currentSpecifier = data.sourceClass.getDelegatorToSuperClassByDescriptor(classDescriptor, data.sourceClassContext) ?: return
        when (data.targetClass) {
            is JetClass -> {
                data.sourceClass.removeDelegationSpecifier(currentSpecifier)
                addDelegatorToSuperClass(currentSpecifier, data.targetClass, data.targetClassDescriptor, data.sourceClassContext, data.sourceToTargetClassSubstitutor)
            }

            is PsiClass -> {
                val elementFactory = JavaPsiFacade.getElementFactory(member.project)

                val sourcePsiClass = data.sourceClass.toLightClass() ?: return
                val superRef = sourcePsiClass.implementsList
                                       ?.referenceElements
                                       ?.firstOrNull { it.resolve()?.unwrapped == member }
                                ?: return
                val superTypeForTarget = substitutor.substitute(elementFactory.createType(superRef))

                data.sourceClass.removeDelegationSpecifier(currentSpecifier)

                if (DescriptorUtils.isSubclass(data.targetClassDescriptor, classDescriptor)) return

                val refList = if (data.isInterfaceTarget) data.targetClass.extendsList else data.targetClass.implementsList
                refList?.add(elementFactory.createReferenceFromText(superTypeForTarget.canonicalText, null))
            }
        }

        return
    }

    private fun removeOriginalMemberOrAddOverride(member: JetCallableDeclaration) {
        if (member.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            member.delete()
        }
        else {
            member.addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
        }
    }

    private fun moveToJavaClass(member: JetNamedDeclaration, substitutor: PsiSubstitutor) {
        if (!(data.targetClass is PsiClass && member.canMoveMemberToJavaClass(data.targetClass))) return

        // TODO: Drop after PsiTypes in light elements are properly generated
        if (member is JetCallableDeclaration && member.typeReference == null) {
            val returnType = (data.memberDescriptors[member] as CallableDescriptor).returnType ?: KotlinBuiltIns.getInstance().anyType
            returnType.anonymousObjectSuperTypeOrNull()?.let { member.setType(it, false) }
        }

        val project = member.project
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val lightMethod = member.getRepresentativeLightMethod()!!

        val movedMember: PsiMember = when (member) {
            is JetProperty -> {
                val newType = substitutor.substitute(lightMethod.returnType)
                val newField = createJavaField(member, data.targetClass)
                newField.typeElement?.replace(elementFactory.createTypeElement(newType))
                member.delete()
                newField
            }
            is JetNamedFunction -> {
                val newReturnType = substitutor.substitute(lightMethod.returnType)
                val newParameterTypes = lightMethod.parameterList.parameters.map { substitutor.substitute(it.type) }
                val objectType = PsiType.getJavaLangObject(PsiManager.getInstance(project), GlobalSearchScope.allScope(project))
                val newTypeParameterBounds = lightMethod.typeParameters.map {
                    it.superTypes.map { substitutor.substitute(it) as? PsiClassType ?: objectType }
                }
                val newMethod = createJavaMethod(member, data.targetClass)
                RefactoringUtil.makeMethodAbstract(data.targetClass, newMethod)
                newMethod.returnTypeElement?.replace(elementFactory.createTypeElement(newReturnType))
                newMethod.parameterList.parameters.forEachIndexed { i, parameter ->
                    parameter.typeElement?.replace(elementFactory.createTypeElement(newParameterTypes[i]))
                }
                newMethod.typeParameters.forEachIndexed { i, typeParameter ->
                    typeParameter.extendsList.referenceElements.forEachIndexed { j, referenceElement ->
                        referenceElement.replace(elementFactory.createReferenceElementByType(newTypeParameterBounds[i][j]))
                    }
                }
                removeOriginalMemberOrAddOverride(member)
                if (!data.isInterfaceTarget && !data.targetClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    data.targetClass.modifierList?.setModifierProperty(PsiModifier.ABSTRACT, true)
                }
                newMethod
            }
            else -> return
        }
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(movedMember)
    }

    override fun move(info: MemberInfoBase<PsiMember>, substitutor: PsiSubstitutor) {
        val member = info.member.namedUnwrappedElement as? JetNamedDeclaration ?: return

        if (member is JetClass && info.overrides != null)  {
            moveSuperInterface(member, substitutor)
            return
        }

        if (data.targetClass is PsiClass) {
            moveToJavaClass(member, substitutor)
            return
        }

        val markedElements = markElements(member, data.sourceClassContext, data.sourceClassDescriptor, data.targetClassDescriptor)
        val memberCopy = member.copy() as JetNamedDeclaration

        fun moveClassOrObject(member: JetClassOrObject, memberCopy: JetClassOrObject): JetClassOrObject {
            if (data.isInterfaceTarget) {
                memberCopy.removeModifier(JetTokens.INNER_KEYWORD)
            }

            val movedMember = addMemberToTarget(memberCopy, data.targetClass as JetClass) as JetClassOrObject
            member.delete()
            return movedMember
        }

        fun moveCallableMember(member: JetCallableDeclaration, memberCopy: JetCallableDeclaration): JetCallableDeclaration {
            data.targetClass as JetClass

            val movedMember: JetCallableDeclaration
            val clashingSuper = fixOverrideAndGetClashingSuper(member, memberCopy)

            val originalIsAbstract = member.hasModifier(JetTokens.ABSTRACT_KEYWORD)
            val toAbstract = when {
                info.isToAbstract -> true
                !data.isInterfaceTarget -> false
                member is JetProperty -> member.mustBeAbstractInInterface()
                else -> false
            }
            if (toAbstract) {
                if (!originalIsAbstract) {
                    makeAbstract(memberCopy, data.memberDescriptors[member] as CallableMemberDescriptor, data.sourceToTargetClassSubstitutor, data.targetClass)
                }

                movedMember = doAddCallableMember(memberCopy, clashingSuper, data.targetClass)
                if (member.typeReference == null) {
                    movedMember.typeReference?.addToShorteningWaitSet()
                }

                removeOriginalMemberOrAddOverride(member)
            }
            else {
                movedMember = doAddCallableMember(memberCopy, clashingSuper, data.targetClass)
                member.delete()
            }

            if (originalIsAbstract && data.isInterfaceTarget) {
                movedMember.removeModifier(JetTokens.ABSTRACT_KEYWORD)
            }

            if (movedMember.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
                data.targetClass.makeAbstract()
            }
            return movedMember
        }

        try {
            val movedMember = when (member) {
                is JetCallableDeclaration -> moveCallableMember(member, memberCopy as JetCallableDeclaration)
                is JetClassOrObject -> moveClassOrObject(member, memberCopy as JetClassOrObject)
                else -> return
            }

            applyMarking(movedMember, data.sourceToTargetClassSubstitutor, data.targetClassDescriptor)
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
            getOrCreateBody().declarations.lastOrNull { it is JetClassInitializer }?.let { return it as JetClassInitializer }
            return addDeclaration(psiFactory.createAnonymousInitializer()) as JetClassInitializer
        }

        fun JetElement.getConstructorBodyBlock(): JetBlockExpression? {
            return when (this) {
                is JetClassOrObject -> {
                    getOrCreateClassInitializer().body
                }
                is JetPrimaryConstructor -> {
                    getContainingClassOrObject().getOrCreateClassInitializer().body
                }
                is JetSecondaryConstructor -> {
                    bodyExpression ?: add(psiFactory.createEmptyBody())
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
                info.usedParameters.forEach {
                    val newParameter = addParameter(it)
                    val originalType = data.sourceClassContext[BindingContext.VALUE_PARAMETER, it]!!.type
                    newParameter.setType(data.sourceToTargetClassSubstitutor.substitute(originalType, Variance.INVARIANT) ?: originalType, false)
                    newParameter.typeReference!!.addToShorteningWaitSet()
                }
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
                superCall?.valueArgumentList?.let { args ->
                    info.usedParameters.forEach {
                        args.addArgument(psiFactory.createArgument(psiFactory.createExpression(it.name ?: "_")))
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
                    body?.addAfter(it, body.statements.lastOrNull() ?: body.lBrace!!)
                }
                info.elementsToRemove.forEach { it.delete() }
            }
        }
    }

    override fun updateUsage(element: PsiElement) {

    }
}
