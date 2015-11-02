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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.refactoring.createJavaField
import org.jetbrains.kotlin.idea.core.refactoring.createJavaMethod
import org.jetbrains.kotlin.idea.core.refactoring.createPrimaryConstructorIfAbsent
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.util.anonymousObjectSuperTypeOrNull
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.lexer.KtTokens
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
    private fun KtExpression.isMovable(): Boolean {
        return accept(
                object: KtVisitor<Boolean, Nothing?>() {
                    override fun visitJetElement(element: KtElement, arg: Nothing?): Boolean {
                        return element.allChildren.all { (it as? KtElement)?.accept(this, arg) ?: true }
                    }

                    override fun visitJetFile(file: KtFile, data: Nothing?) = false

                    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, arg: Nothing?): Boolean {
                        val resolvedCall = expression.getResolvedCall(data.resolutionFacade.analyze(expression)) ?: return true
                        val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.expression
                        if (receiver != null && receiver !is KtThisExpression && receiver !is KtSuperExpression) return true

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
            currentInitializer: KtExpression?,
            scope: KtBlockExpression?,
            propertyDescriptor: PropertyDescriptor,
            elementsToRemove: MutableSet<KtElement>): KtExpression? {
        if (scope == null) return currentInitializer

        var initializerCandidate: KtExpression? = null

        for (statement in scope.statements) {
            statement.asAssignment()?.let body@ {
                val lhs = KtPsiUtil.safeDeparenthesize(it.left ?: return@body)
                val receiver = (lhs as? KtQualifiedExpression)?.receiverExpression
                if (receiver != null && receiver !is KtThisExpression) return@body

                val resolvedCall = lhs.getResolvedCall(data.resolutionFacade.analyze(it)) ?: return@body
                if (resolvedCall.resultingDescriptor != propertyDescriptor) return@body

                if (initializerCandidate == null) {
                    if (currentInitializer == null) {
                        if (!statement.isMovable()) return null

                        initializerCandidate = statement
                        elementsToRemove.add(statement)
                    }
                    else {
                        if (!KotlinPsiUnifier.DEFAULT.unify(statement, currentInitializer).matched) return null

                        initializerCandidate = currentInitializer
                        elementsToRemove.add(statement)
                    }
                }
                else if (!KotlinPsiUnifier.DEFAULT.unify(statement, initializerCandidate).matched) return null
            }
        }

        return initializerCandidate
    }

    private data class InitializerInfo(
            val initializer: KtExpression?,
            val usedProperties: Set<KtProperty>,
            val usedParameters: Set<KtParameter>,
            val elementsToRemove: Set<KtElement>
    )

    private fun getInitializerInfo(property: KtProperty,
                                   propertyDescriptor: PropertyDescriptor,
                                   targetConstructor: KtElement): InitializerInfo? {
        val sourceConstructors = targetToSourceConstructors[targetConstructor] ?: return null
        val elementsToRemove = LinkedHashSet<KtElement>()
        val commonInitializer = sourceConstructors.fold(null as KtExpression?) { commonInitializer, constructor ->
            val body = (constructor as? KtSecondaryConstructor)?.bodyExpression
            getCommonInitializer(commonInitializer, body, propertyDescriptor, elementsToRemove)
        }
        if (commonInitializer == null) {
            elementsToRemove.clear()
        }

        val usedProperties = LinkedHashSet<KtProperty>()
        val usedParameters = LinkedHashSet<KtParameter>()
        val visitor = object : KtTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                val context = data.resolutionFacade.analyze(expression)
                val resolvedCall = expression.getResolvedCall(context) ?: return
                val receiver = (resolvedCall.getExplicitReceiverValue() as? ExpressionReceiver)?.expression
                if (receiver != null && receiver !is KtThisExpression) return
                val target = (resolvedCall.resultingDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi()
                when (target) {
                    is KtParameter -> usedParameters.add(target)
                    is KtProperty -> usedProperties.add(target)
                }
            }
        }
        commonInitializer?.accept(visitor)
        if (targetConstructor == (data.targetClass as? KtClass)?.getPrimaryConstructor() ?: data.targetClass) {
            property.initializer?.accept(visitor)
        }

        return InitializerInfo(commonInitializer, usedProperties, usedParameters, elementsToRemove)
    }

    private val propertiesToMoveInitializers = with(data) {
        membersToMove
                .filterIsInstance<KtProperty>()
                .filter {
                    val descriptor = memberDescriptors[it] as? PropertyDescriptor
                    descriptor != null && data.sourceClassContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] ?: false
                }
    }

    private val targetToSourceConstructors = LinkedHashMap<KtElement, MutableList<KtElement>>().let { result ->
        if (!data.isInterfaceTarget && data.targetClass is KtClass) {
            result[data.targetClass.getPrimaryConstructor() ?: data.targetClass] = ArrayList<KtElement>()
            data.sourceClass.accept(
                    object : KtTreeVisitorVoid() {
                        private fun processConstructorReference(expression: KtReferenceExpression, callingConstructorElement: KtElement) {
                            val descriptor = data.resolutionFacade.analyze(expression)[BindingContext.REFERENCE_TARGET, expression]
                            val constructorElement = (descriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() ?: return
                            if (constructorElement == data.targetClass
                                || (constructorElement as? KtConstructor<*>)?.getContainingClassOrObject() == data.targetClass) {
                                result.getOrPut(constructorElement as KtElement, { ArrayList() }).add(callingConstructorElement)
                            }
                        }

                        override fun visitDelegationToSuperCallSpecifier(specifier: KtDelegatorToSuperCall) {
                            val constructorRef = specifier.calleeExpression.constructorReferenceExpression ?: return
                            val containingClass = specifier.getStrictParentOfType<KtClassOrObject>() ?: return
                            val callingConstructorElement = containingClass.getPrimaryConstructor() ?: containingClass
                            processConstructorReference(constructorRef, callingConstructorElement)
                        }

                        override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
                            val constructorRef = constructor.getDelegationCall().calleeExpression ?: return
                            processConstructorReference(constructorRef, constructor)
                        }
                    }
            )
        }
        result
    }

    private val targetConstructorToPropertyInitializerInfoMap = LinkedHashMap<KtElement, Map<KtProperty, InitializerInfo>>().let { result ->
        for (targetConstructor in targetToSourceConstructors.keySet()) {
            val propertyToInitializerInfo = LinkedHashMap<KtProperty, InitializerInfo>()
            for (property in propertiesToMoveInitializers) {
                val propertyDescriptor = data.memberDescriptors[property] as? PropertyDescriptor ?: continue
                propertyToInitializerInfo[property] = getInitializerInfo(property, propertyDescriptor, targetConstructor) ?: continue
            }
            val unmovableProperties = RefactoringUtil.transitiveClosure(
                    object : RefactoringUtil.Graph<KtProperty> {
                        override fun getVertices() = propertyToInitializerInfo.keySet()

                        override fun getTargets(source: KtProperty) = propertyToInitializerInfo[source]?.usedProperties
                    },
                    { !propertyToInitializerInfo.containsKey(it) }
            )

            propertyToInitializerInfo.keySet().removeAll(unmovableProperties)
            result[targetConstructor] = propertyToInitializerInfo
        }
        result
    }

    private var dummyField: PsiField? = null

    private fun addMovedMember(newMember: KtNamedDeclaration) {
        if (newMember is KtProperty) {
            // Add dummy light field since PullUpProcessor won't invoke moveFieldInitializations() if no PsiFields are present
            if (dummyField == null) {
                val factory = JavaPsiFacade.getElementFactory(newMember.project)
                val dummyField = object: LightField(
                        newMember.manager,
                        factory.createField("dummy", PsiType.BOOLEAN),
                        factory.createClass("Dummy")
                ) {
                    // Prevent processing by JavaPullUpHelper
                    override fun getLanguage() = KotlinLanguage.INSTANCE
                }
                javaData.movedMembers.add(dummyField)
            }
        }

        when (newMember) {
            is KtProperty, is KtNamedFunction -> {
                newMember.getRepresentativeLightMethod()?.let { javaData.movedMembers.add(it) }
            }
            is KtClassOrObject -> {
                newMember.toLightClass()?.let { javaData.movedMembers.add(it) }
            }
        }
    }

    private fun willBeUsedInSourceClass(member: PsiElement): Boolean {
        return !ReferencesSearch
                .search(member, LocalSearchScope(data.sourceClass), false)
                .all { it.element.parentsWithSelf.any { it in data.membersToMove } }
    }

    private fun liftToProtected(declaration: KtNamedDeclaration, ignoreUsages: Boolean = false) {
        if (!declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) return
        if (ignoreUsages || willBeUsedInSourceClass(declaration)) declaration.addModifierWithSpace(KtTokens.PROTECTED_KEYWORD)
    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {
        val member = info.member.namedUnwrappedElement as? KtNamedDeclaration ?: return

        if (data.isInterfaceTarget) {
            member.removeModifier(KtTokens.PUBLIC_KEYWORD)
            return
        }

        if (member.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            member.accept(
                    object: KtVisitorVoid() {
                        override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                            when (declaration) {
                                is KtClass -> {
                                    liftToProtected(declaration)
                                    declaration.declarations.forEach { it.accept(this) }
                                }
                                is KtNamedFunction, is KtProperty -> {
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

    private fun fixOverrideAndGetClashingSuper(sourceMember: KtCallableDeclaration,
                                               targetMember: KtCallableDeclaration): KtCallableDeclaration? {
        val memberDescriptor = data.memberDescriptors[sourceMember] as CallableMemberDescriptor

        if (memberDescriptor.overriddenDescriptors.isEmpty()) {
            targetMember.removeOverrideModifier()
            return null
        }

        val clashingSuperDescriptor = data.getClashingMemberInTargetClass(memberDescriptor) ?: return null
        if (clashingSuperDescriptor.overriddenDescriptors.isEmpty()) {
            targetMember.removeOverrideModifier()
        }
        return clashingSuperDescriptor.source.getPsi() as? KtCallableDeclaration
    }

    private fun moveSuperInterface(member: KtClass, substitutor: PsiSubstitutor) {
        val classDescriptor = data.memberDescriptors[member] as? ClassDescriptor ?: return
        val currentSpecifier = data.sourceClass.getDelegatorToSuperClassByDescriptor(classDescriptor, data.sourceClassContext) ?: return
        when (data.targetClass) {
            is KtClass -> {
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

    private fun removeOriginalMemberOrAddOverride(member: KtCallableDeclaration) {
        if (member.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
            member.delete()
        }
        else {
            member.addModifierWithSpace(KtTokens.OVERRIDE_KEYWORD)
        }
    }

    private fun moveToJavaClass(member: KtNamedDeclaration, substitutor: PsiSubstitutor) {
        if (!(data.targetClass is PsiClass && member.canMoveMemberToJavaClass(data.targetClass))) return

        // TODO: Drop after PsiTypes in light elements are properly generated
        if (member is KtCallableDeclaration && member.typeReference == null) {
            val returnType = (data.memberDescriptors[member] as CallableDescriptor).returnType
            returnType?.anonymousObjectSuperTypeOrNull()?.let { member.setType(it, false) }
        }

        val project = member.project
        val elementFactory = JavaPsiFacade.getElementFactory(project)
        val lightMethod = member.getRepresentativeLightMethod()!!

        val movedMember: PsiMember = when (member) {
            is KtProperty -> {
                val newType = substitutor.substitute(lightMethod.returnType)
                val newField = createJavaField(member, data.targetClass)
                newField.typeElement?.replace(elementFactory.createTypeElement(newType))
                member.delete()
                newField
            }
            is KtNamedFunction -> {
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
        val member = info.member.namedUnwrappedElement as? KtNamedDeclaration ?: return

        if (member is KtClass && info.overrides != null)  {
            moveSuperInterface(member, substitutor)
            return
        }

        if (data.targetClass is PsiClass) {
            moveToJavaClass(member, substitutor)
            return
        }

        val markedElements = markElements(member, data.sourceClassContext, data.sourceClassDescriptor, data.targetClassDescriptor)
        val memberCopy = member.copy() as KtNamedDeclaration

        fun moveClassOrObject(member: KtClassOrObject, memberCopy: KtClassOrObject): KtClassOrObject {
            if (data.isInterfaceTarget) {
                memberCopy.removeModifier(KtTokens.INNER_KEYWORD)
            }

            val movedMember = addMemberToTarget(memberCopy, data.targetClass as KtClass) as KtClassOrObject
            member.delete()
            return movedMember
        }

        fun moveCallableMember(member: KtCallableDeclaration, memberCopy: KtCallableDeclaration): KtCallableDeclaration {
            data.targetClass as KtClass

            val movedMember: KtCallableDeclaration
            val clashingSuper = fixOverrideAndGetClashingSuper(member, memberCopy)

            val originalIsAbstract = member.hasModifier(KtTokens.ABSTRACT_KEYWORD)
            val toAbstract = when {
                info.isToAbstract -> true
                !data.isInterfaceTarget -> false
                member is KtProperty -> member.mustBeAbstractInInterface()
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
                movedMember.removeModifier(KtTokens.ABSTRACT_KEYWORD)
            }

            if (movedMember.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                data.targetClass.makeAbstract()
            }
            return movedMember
        }

        try {
            val movedMember = when (member) {
                is KtCallableDeclaration -> moveCallableMember(member, memberCopy as KtCallableDeclaration)
                is KtClassOrObject -> moveClassOrObject(member, memberCopy as KtClassOrObject)
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
        val psiFactory = KtPsiFactory(data.sourceClass)

        fun KtClassOrObject.getOrCreateClassInitializer(): KtClassInitializer {
            getOrCreateBody().declarations.lastOrNull { it is KtClassInitializer }?.let { return it as KtClassInitializer }
            return addDeclaration(psiFactory.createAnonymousInitializer()) as KtClassInitializer
        }

        fun KtElement.getConstructorBodyBlock(): KtBlockExpression? {
            return when (this) {
                is KtClassOrObject -> {
                    getOrCreateClassInitializer().body
                }
                is KtPrimaryConstructor -> {
                    getContainingClassOrObject().getOrCreateClassInitializer().body
                }
                is KtSecondaryConstructor -> {
                    bodyExpression ?: add(psiFactory.createEmptyBody())
                }
                else -> null
            } as? KtBlockExpression
        }

        fun KtClassOrObject.getDelegatorToSuperCall(): KtDelegatorToSuperCall? {
            return getDelegationSpecifiers().singleOrNull { it is KtDelegatorToSuperCall } as? KtDelegatorToSuperCall
        }

        fun addUsedParameters(constructorElement: KtElement, info: InitializerInfo) {
            if (!info.usedParameters.isNotEmpty()) return
            val constructor: KtConstructor<*> = when (constructorElement) {
                is KtConstructor<*> -> constructorElement
                is KtClass -> constructorElement.createPrimaryConstructorIfAbsent()
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
                val superCall: KtCallElement? = when (it) {
                    is KtClassOrObject -> it.getDelegatorToSuperCall()
                    is KtPrimaryConstructor -> it.getContainingClassOrObject().getDelegatorToSuperCall()
                    is KtSecondaryConstructor -> {
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
            val properties = propertyToInitializerInfo.keySet().sortedWith(
                    object : Comparator<KtProperty> {
                        override fun compare(property1: KtProperty, property2: KtProperty): Int {
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
