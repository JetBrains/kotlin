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
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.light.LightField
import com.intellij.psi.search.GlobalSearchScope
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
import org.jetbrains.kotlin.idea.core.dropDefaultValue
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.inspections.CONSTRUCTOR_VAL_VAR_MODIFIERS
import org.jetbrains.kotlin.idea.refactoring.createJavaField
import org.jetbrains.kotlin.idea.refactoring.dropOverrideKeywordIfNecessary
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.refactoring.isCompanionMemberOf
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.idea.refactoring.memberInfo.toKtDeclarationWrapperAware
import org.jetbrains.kotlin.idea.refactoring.safeDelete.removeOverrideModifier
import org.jetbrains.kotlin.idea.util.anonymousObjectSuperTypeOrNull
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
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
    companion object {
        private val MODIFIERS_TO_LIFT_IN_SUPERCLASS = listOf(KtTokens.PRIVATE_KEYWORD)
        private val MODIFIERS_TO_LIFT_IN_INTERFACE = listOf(KtTokens.PRIVATE_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.INTERNAL_KEYWORD)
    }

    private fun KtExpression.isMovable(): Boolean {
        return accept(
            object : KtVisitor<Boolean, Nothing?>() {
                override fun visitKtElement(element: KtElement, arg: Nothing?): Boolean {
                    return element.allChildren.all { (it as? KtElement)?.accept(this, arg) ?: true }
                }

                override fun visitKtFile(file: KtFile, data: Nothing?) = false

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
        elementsToRemove: MutableSet<KtElement>
    ): KtExpression? {
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
                    } else {
                        if (!KotlinPsiUnifier.DEFAULT.unify(statement, currentInitializer).matched) return null

                        initializerCandidate = currentInitializer
                        elementsToRemove.add(statement)
                    }
                } else if (!KotlinPsiUnifier.DEFAULT.unify(statement, initializerCandidate).matched) return null
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

    private fun getInitializerInfo(
        property: KtProperty,
        propertyDescriptor: PropertyDescriptor,
        targetConstructor: KtElement
    ): InitializerInfo? {
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
        if (targetConstructor == (data.targetClass as? KtClass)?.primaryConstructor ?: data.targetClass) {
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
            result[data.targetClass.primaryConstructor ?: data.targetClass] = ArrayList()
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

                    override fun visitSuperTypeCallEntry(specifier: KtSuperTypeCallEntry) {
                        val constructorRef = specifier.calleeExpression.constructorReferenceExpression ?: return
                        val containingClass = specifier.getStrictParentOfType<KtClassOrObject>() ?: return
                        val callingConstructorElement = containingClass.primaryConstructor ?: containingClass
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
        for (targetConstructor in targetToSourceConstructors.keys) {
            val propertyToInitializerInfo = LinkedHashMap<KtProperty, InitializerInfo>()
            for (property in propertiesToMoveInitializers) {
                val propertyDescriptor = data.memberDescriptors[property] as? PropertyDescriptor ?: continue
                propertyToInitializerInfo[property] = getInitializerInfo(property, propertyDescriptor, targetConstructor) ?: continue
            }
            val unmovableProperties = RefactoringUtil.transitiveClosure(
                object : RefactoringUtil.Graph<KtProperty> {
                    override fun getVertices() = propertyToInitializerInfo.keys

                    override fun getTargets(source: KtProperty) = propertyToInitializerInfo[source]?.usedProperties
                },
                { !propertyToInitializerInfo.containsKey(it) }
            )

            propertyToInitializerInfo.keys.removeAll(unmovableProperties)
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
                val dummyField = object : LightField(
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

    private fun liftVisibility(declaration: KtNamedDeclaration, ignoreUsages: Boolean = false) {
        val newModifier = if (data.isInterfaceTarget) KtTokens.PUBLIC_KEYWORD else KtTokens.PROTECTED_KEYWORD
        val modifiersToLift = if (data.isInterfaceTarget) MODIFIERS_TO_LIFT_IN_INTERFACE else MODIFIERS_TO_LIFT_IN_SUPERCLASS
        val currentModifier = declaration.visibilityModifierTypeOrDefault()
        if (currentModifier !in modifiersToLift) return
        if (ignoreUsages || willBeUsedInSourceClass(declaration, data.sourceClass, data.membersToMove)) {
            if (newModifier != KtTokens.DEFAULT_VISIBILITY_KEYWORD) {
                declaration.addModifier(newModifier)
            } else {
                declaration.removeModifier(currentModifier)
            }
        }
    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {
        val member = info.member.namedUnwrappedElement as? KtNamedDeclaration ?: return

        if (data.isInterfaceTarget) {
            member.removeModifier(KtTokens.PUBLIC_KEYWORD)
        }

        val modifiersToLift = if (data.isInterfaceTarget) MODIFIERS_TO_LIFT_IN_INTERFACE else MODIFIERS_TO_LIFT_IN_SUPERCLASS
        if (member.visibilityModifierTypeOrDefault() in modifiersToLift) {
            member.accept(
                object : KtVisitorVoid() {
                    override fun visitNamedDeclaration(declaration: KtNamedDeclaration) {
                        when (declaration) {
                            is KtClass -> {
                                liftVisibility(declaration)
                                declaration.declarations.forEach { it.accept(this) }
                            }
                            is KtNamedFunction, is KtProperty -> {
                                liftVisibility(declaration, declaration == member && info.isToAbstract)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun encodeContextInfo(info: MemberInfoBase<PsiMember>) {

    }

    private fun fixOverrideAndGetClashingSuper(
        sourceMember: KtCallableDeclaration,
        targetMember: KtCallableDeclaration
    ): KtCallableDeclaration? {
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

    private fun moveSuperInterface(member: PsiNamedElement, substitutor: PsiSubstitutor) {
        val realMemberPsi = (member as? KtPsiClassWrapper)?.psiClass ?: member
        val classDescriptor = data.memberDescriptors[member] as? ClassDescriptor ?: return
        val currentSpecifier = data.sourceClass.getSuperTypeEntryByDescriptor(classDescriptor, data.sourceClassContext) ?: return
        when (data.targetClass) {
            is KtClass -> {
                data.sourceClass.removeSuperTypeListEntry(currentSpecifier)
                addSuperTypeEntry(
                    currentSpecifier,
                    data.targetClass,
                    data.targetClassDescriptor,
                    data.sourceClassContext,
                    data.sourceToTargetClassSubstitutor
                )
            }

            is PsiClass -> {
                val elementFactory = JavaPsiFacade.getElementFactory(member.project)

                val sourcePsiClass = data.sourceClass.toLightClass() ?: return
                val superRef = sourcePsiClass.implementsList
                    ?.referenceElements
                    ?.firstOrNull { it.resolve()?.unwrapped == realMemberPsi }
                        ?: return
                val superTypeForTarget = substitutor.substitute(elementFactory.createType(superRef))

                data.sourceClass.removeSuperTypeListEntry(currentSpecifier)

                if (DescriptorUtils.isSubclass(data.targetClassDescriptor, classDescriptor)) return

                val refList = if (data.isInterfaceTarget) data.targetClass.extendsList else data.targetClass.implementsList
                refList?.add(elementFactory.createReferenceFromText(superTypeForTarget.canonicalText, null))
            }
        }

        return
    }

    private fun removeOriginalMemberOrAddOverride(member: KtCallableDeclaration) {
        if (member.isAbstract()) {
            member.deleteWithCompanion()
        } else {
            member.addModifier(KtTokens.OVERRIDE_KEYWORD)
            KtTokens.VISIBILITY_MODIFIERS.types.forEach { member.removeModifier(it as KtModifierKeywordToken) }
            (member as? KtNamedFunction)?.valueParameters?.forEach { it.dropDefaultValue() }
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
            is KtProperty, is KtParameter -> {
                val newType = substitutor.substitute(lightMethod.returnType)
                val newField = createJavaField(member, data.targetClass)
                newField.typeElement?.replace(elementFactory.createTypeElement(newType))
                if (member.isCompanionMemberOf(data.sourceClass)) {
                    newField.modifierList?.setModifierProperty(PsiModifier.STATIC, true)
                }
                if (member is KtParameter) {
                    (member.parent as? KtParameterList)?.removeParameter(member)
                } else {
                    member.deleteWithCompanion()
                }
                newField
            }
            is KtNamedFunction -> {
                val newReturnType = substitutor.substitute(lightMethod.returnType)
                val newParameterTypes = lightMethod.parameterList.parameters.map { substitutor.substitute(it.type) }
                val objectType = PsiType.getJavaLangObject(PsiManager.getInstance(project), GlobalSearchScope.allScope(project))
                val newTypeParameterBounds = lightMethod.typeParameters.map {
                    it.superTypes.map { substitutor.substitute(it) as? PsiClassType ?: objectType }
                }
                val newMethod = org.jetbrains.kotlin.idea.refactoring.createJavaMethod(member, data.targetClass)
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
        val member = info.member.toKtDeclarationWrapperAware() ?: return

        if ((member is KtClass || member is KtPsiClassWrapper) && info.overrides != null) {
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
            member.deleteWithCompanion()
            return movedMember
        }

        fun moveCallableMember(member: KtCallableDeclaration, memberCopy: KtCallableDeclaration): KtCallableDeclaration {
            data.targetClass as KtClass

            val movedMember: KtCallableDeclaration
            val clashingSuper = fixOverrideAndGetClashingSuper(member, memberCopy)

            val psiFactory = KtPsiFactory(member)

            val originalIsAbstract = member.hasModifier(KtTokens.ABSTRACT_KEYWORD)
            val toAbstract = when {
                info.isToAbstract -> true
                !data.isInterfaceTarget -> false
                member is KtProperty -> member.mustBeAbstractInInterface()
                else -> false
            }

            val classToAddTo =
                if (member.isCompanionMemberOf(data.sourceClass)) data.targetClass.getOrCreateCompanionObject() else data.targetClass

            if (toAbstract) {
                if (!originalIsAbstract) {
                    makeAbstract(
                        memberCopy,
                        data.memberDescriptors[member] as CallableMemberDescriptor,
                        data.sourceToTargetClassSubstitutor,
                        data.targetClass
                    )
                }

                movedMember = doAddCallableMember(memberCopy, clashingSuper, classToAddTo)
                if (member.typeReference == null) {
                    movedMember.typeReference?.addToShorteningWaitSet()
                }

                removeOriginalMemberOrAddOverride(member)
            } else {
                movedMember = doAddCallableMember(memberCopy, clashingSuper, classToAddTo)
                if (member is KtParameter && movedMember is KtParameter) {
                    member.valOrVarKeyword?.delete()
                    CONSTRUCTOR_VAL_VAR_MODIFIERS.forEach { member.removeModifier(it) }

                    val superEntry = data.superEntryForTargetClass
                    val superResolvedCall = data.targetClassSuperResolvedCall
                    if (superResolvedCall != null) {
                        val superCall = if (superEntry !is KtSuperTypeCallEntry || superEntry.valueArgumentList == null) {
                            superEntry!!.replaced(psiFactory.createSuperTypeCallEntry("${superEntry.text}()"))
                        } else superEntry
                        val argumentList = superCall.valueArgumentList!!

                        val parameterIndex = movedMember.parameterIndex()
                        val prevParameterDescriptor = superResolvedCall.resultingDescriptor.valueParameters.getOrNull(parameterIndex - 1)
                        val prevArgument =
                            superResolvedCall.valueArguments[prevParameterDescriptor]?.arguments?.singleOrNull() as? KtValueArgument
                        val newArgumentName = if (prevArgument != null && prevArgument.isNamed()) Name.identifier(member.name!!) else null
                        val newArgument = psiFactory.createArgument(psiFactory.createExpression(member.name!!), newArgumentName)
                        if (prevArgument == null) {
                            argumentList.addArgument(newArgument)
                        } else {
                            argumentList.addArgumentAfter(newArgument, prevArgument)
                        }
                    }
                } else {
                    member.deleteWithCompanion()
                }
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

            movedMember.modifierList?.let { CodeStyleManager.getInstance(member.manager).reformat(it) }

            applyMarking(movedMember, data.sourceToTargetClassSubstitutor, data.targetClassDescriptor)
            addMovedMember(movedMember)
        } finally {
            clearMarking(markedElements)
        }
    }

    override fun postProcessMember(member: PsiMember) {
        val declaration = member.unwrapped as? KtNamedDeclaration ?: return
        dropOverrideKeywordIfNecessary(declaration)
    }

    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>) {
        val psiFactory = KtPsiFactory(data.sourceClass)

        fun KtClassOrObject.getOrCreateClassInitializer(): KtAnonymousInitializer {
            getOrCreateBody().declarations.lastOrNull { it is KtAnonymousInitializer }?.let { return it as KtAnonymousInitializer }
            return addDeclaration(psiFactory.createAnonymousInitializer())
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

        fun KtClassOrObject.getDelegatorToSuperCall(): KtSuperTypeCallEntry? {
            return superTypeListEntries.singleOrNull { it is KtSuperTypeCallEntry } as? KtSuperTypeCallEntry
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
                    newParameter.setType(
                        data.sourceToTargetClassSubstitutor.substitute(originalType, Variance.INVARIANT) ?: originalType,
                        false
                    )
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
                        } else {
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

        for ((constructorElement, propertyToInitializerInfo) in targetConstructorToPropertyInitializerInfoMap.entries) {
            val properties = propertyToInitializerInfo.keys.sortedWith(
                Comparator { property1, property2 ->
                    val info1 = propertyToInitializerInfo[property1]!!
                    val info2 = propertyToInitializerInfo[property2]!!
                    when {
                        property2 in info1.usedProperties -> -1
                        property1 in info2.usedProperties -> 1
                        else -> 0
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

internal fun KtNamedDeclaration.deleteWithCompanion() {
    val containingClass = this.containingClassOrObject
    if (containingClass is KtObjectDeclaration && containingClass.isCompanion() && containingClass.declarations.size == 1) {
        containingClass.delete()
    } else {
        this.delete()
    }
}

