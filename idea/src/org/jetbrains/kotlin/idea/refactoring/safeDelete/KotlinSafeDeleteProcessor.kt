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

package org.jetbrains.kotlin.idea.refactoring.safeDelete

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverrideAnnotation
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverridingMethodUsageInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceJavaDeleteUsageInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.deleteElementAndCleanParent
import org.jetbrains.kotlin.idea.highlighter.markers.actualsForExpected
import org.jetbrains.kotlin.idea.highlighter.markers.liftToExpected
import org.jetbrains.kotlin.idea.refactoring.checkSuperMethods
import org.jetbrains.kotlin.idea.refactoring.formatClass
import org.jetbrains.kotlin.idea.refactoring.formatFunction
import org.jetbrains.kotlin.idea.refactoring.withExpectedActuals
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

class KotlinSafeDeleteProcessor : JavaSafeDeleteProcessor() {
    companion object {
        @set:TestOnly
        internal var Project.ALLOW_LIFTING_ACTUAL_PARAMETER_TO_EXPECTED
                by NotNullableUserDataProperty(Key.create("ALLOW_LIFTING_ACTUAL_PARAMETER_TO_EXPECTED"), true)

        private var KtDeclaration.dropActualModifier: Boolean? by UserDataProperty(Key.create("DROP_ACTUAL_MODIFIER"))
    }

    override fun handlesElement(element: PsiElement): Boolean = element.canDeleteElement()

    override fun findUsages(
            element: PsiElement, allElementsToDelete: Array<out PsiElement>, usages: MutableList<UsageInfo>
    ): NonCodeUsageSearchInfo {
        val deleteSet = SmartSet.create<PsiElement>()
        deleteSet.addAll(allElementsToDelete)

        fun getIgnoranceCondition() = Condition<PsiElement> {
            if (it is KtFile) return@Condition false
            deleteSet.any { element -> JavaSafeDeleteProcessor.isInside(it, element.unwrapped) }
        }

        fun getSearchInfo(element: PsiElement) = NonCodeUsageSearchInfo(getIgnoranceCondition(), element)

        fun searchKotlinDeclarationReferences(declaration: KtDeclaration): Sequence<PsiReference> {
            val elementsToSearch = if (declaration is KtParameter) declaration.withExpectedActuals() else listOf(declaration)
            return elementsToSearch.asSequence().flatMap {
                val searchParameters = KotlinReferencesSearchParameters(
                        it,
                        it.useScope,
                        kotlinOptions = KotlinReferencesSearchOptions(acceptCallableOverrides = true)
                )
                ReferencesSearch.search(searchParameters)
                        .asSequence()
                        .filterNot { reference -> getIgnoranceCondition().value(reference.element) }
            }
        }

        fun findKotlinParameterUsages(parameter: KtParameter) {
            val ownerFunction = parameter.ownerFunction as? KtFunction ?: return
            val index = parameter.parameterIndex()
            for (reference in searchKotlinDeclarationReferences(ownerFunction)) {
                val callee = reference.element as? KtExpression ?: continue
                val resolvedCall = callee.getResolvedCall(callee.analyze()) ?: continue
                val parameterDescriptor = resolvedCall.candidateDescriptor.valueParameters.getOrNull(index) ?: continue
                val resolvedArgument = resolvedCall.valueArguments[parameterDescriptor] ?: continue
                val arguments = resolvedArgument.arguments.filterIsInstance<KtValueArgument>()
                if (arguments.isEmpty()) continue

                usages.add(SafeDeleteValueArgumentListUsageInfo(parameter, *arguments.toTypedArray()))
            }
        }

        fun findKotlinDeclarationUsages(declaration: KtDeclaration): NonCodeUsageSearchInfo {
            searchKotlinDeclarationReferences(declaration).mapNotNullTo(usages) { reference ->
                val refElement = reference.element ?: return@mapNotNullTo null
                refElement.getNonStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                    SafeDeleteImportDirectiveUsageInfo(importDirective, element)
                } ?: SafeDeleteReferenceSimpleDeleteUsageInfo(refElement, declaration, false)
            }

            if (declaration is KtParameter) {
                findKotlinParameterUsages(declaration)
            }

            return getSearchInfo(declaration)
        }

        fun findUsagesByJavaProcessor(element: PsiElement, forceReferencedElementUnwrapping: Boolean): NonCodeUsageSearchInfo? {
            val javaUsages = ArrayList<UsageInfo>()
            val searchInfo = super.findUsages(element, allElementsToDelete, javaUsages)

            javaUsages.filterIsInstance<SafeDeleteOverridingMethodUsageInfo>().mapNotNullTo(deleteSet) { it.element }

            val ignoranceCondition = getIgnoranceCondition()

            javaUsages.mapNotNullTo(usages) { usageInfo ->
                when (usageInfo) {
                    is SafeDeleteOverridingMethodUsageInfo ->
                        usageInfo.smartPointer.element?.let { usageElement ->
                            KotlinSafeDeleteOverridingUsageInfo(usageElement, usageInfo.referencedElement)
                        }

                    is SafeDeleteOverrideAnnotation ->
                        usageInfo.smartPointer.element?.let { usageElement ->
                            if (usageElement.toLightMethods().all { method -> method.findSuperMethods().isEmpty() }) {
                                KotlinSafeDeleteOverrideAnnotation(usageElement, usageInfo.referencedElement)
                            }
                            else null
                        }

                    is SafeDeleteReferenceJavaDeleteUsageInfo ->
                        usageInfo.element?.let { usageElement ->
                            when {
                                usageElement.getNonStrictParentOfType<KtValueArgumentName>() != null -> null
                                ignoranceCondition.value(usageElement) -> null
                                else -> {
                                    usageElement.getNonStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                                        SafeDeleteImportDirectiveUsageInfo(importDirective, element)
                                    } ?: usageElement.getParentOfTypeAndBranch<KtSuperTypeEntry> { typeReference }?.let {
                                        if (element is PsiClass && element.isInterface) SafeDeleteSuperTypeUsageInfo(it, element) else usageInfo
                                    } ?: if (forceReferencedElementUnwrapping) {
                                        SafeDeleteReferenceJavaDeleteUsageInfo(usageElement, element.unwrapped, usageInfo.isSafeDelete)
                                    } else usageInfo
                                }
                            }
                        }

                    else -> usageInfo
                }
            }

            return searchInfo
        }

        fun findUsagesByJavaProcessor(elements: Sequence<PsiElement>, insideDeleted: Condition<PsiElement>): Condition<PsiElement> =
                elements
                        .mapNotNull { element -> findUsagesByJavaProcessor(element, true)?.insideDeletedCondition }
                        .fold(insideDeleted) { condition1, condition2 -> Conditions.or(condition1, condition2) }

        fun findUsagesByJavaProcessor(ktDeclaration: KtDeclaration): NonCodeUsageSearchInfo {
            val lightElements = ktDeclaration.toLightElements()
            if (lightElements.isEmpty()) {
                return findKotlinDeclarationUsages(ktDeclaration)
            }
            return NonCodeUsageSearchInfo(
                    findUsagesByJavaProcessor(
                            lightElements.asSequence(),
                            getIgnoranceCondition()
                    ),
                    ktDeclaration
            )
        }

        fun findTypeParameterUsages(parameter: KtTypeParameter) {
            val owner = parameter.getNonStrictParentOfType<KtTypeParameterListOwner>() ?: return

            val parameterList = owner.typeParameters
            val parameterIndex = parameterList.indexOf(parameter)

            for (reference in ReferencesSearch.search(owner)) {
                if (reference !is KtReference) continue

                val referencedElement = reference.element

                val argList = referencedElement.getNonStrictParentOfType<KtUserType>()?.typeArgumentList
                              ?: referencedElement.getNonStrictParentOfType<KtCallExpression>()?.typeArgumentList

                if (argList != null) {
                    val projections = argList.arguments
                    if (parameterIndex < projections.size) {
                        usages.add(SafeDeleteTypeArgumentListUsageInfo(projections[parameterIndex], parameter))
                    }
                }
            }
        }

        fun findDelegationCallUsages(element: PsiElement) {
            val constructors = when (element) {
                is PsiClass -> element.constructors
                is PsiMethod -> arrayOf(element)
                else -> return
            }
            for (constructor in constructors) {
                constructor.processDelegationCallConstructorUsages(constructor.useScope) {
                    if (!getIgnoranceCondition().value(it)) {
                        usages.add(SafeDeleteReferenceSimpleDeleteUsageInfo(it, element, false))
                    }
                    true
                }
            }
        }

        return when (element) {
            is KtClassOrObject -> {
                element.toLightClass()?.let { klass ->
                    findDelegationCallUsages(klass)
                    findUsagesByJavaProcessor(klass, false)
                } ?: findKotlinDeclarationUsages(element)
            }

            is KtSecondaryConstructor -> {
                element.getRepresentativeLightMethod()?.let { method ->
                    findDelegationCallUsages(method)
                    findUsagesByJavaProcessor(method, false)
                } ?: findKotlinDeclarationUsages(element)
            }

            is KtNamedFunction -> {
                if (element.isLocal) {
                    findKotlinDeclarationUsages(element)
                }
                else {
                    val lightMethods = element.toLightMethods()
                    if (lightMethods.isNotEmpty()) {
                        lightMethods.map { method -> findUsagesByJavaProcessor(method, false) }.firstOrNull()
                    }
                    else {
                        findKotlinDeclarationUsages(element)
                    }
                }
            }

            is PsiMethod -> {
                findUsagesByJavaProcessor(element, false)
            }

            is PsiClass -> {
                findUsagesByJavaProcessor(element, false)
            }

            is KtProperty -> {
                if (element.isLocal) {
                    findKotlinDeclarationUsages(element)
                }
                else {
                    findUsagesByJavaProcessor(element)
                }
            }

            is KtTypeParameter -> {
                findTypeParameterUsages(element)
                findUsagesByJavaProcessor(element)
            }

            is KtParameter ->
                findUsagesByJavaProcessor(element)

            is KtTypeAlias -> {
                findKotlinDeclarationUsages(element)
            }

            else -> null
        } ?: getSearchInfo(element)
    }

    override fun findConflicts(element: PsiElement, allElementsToDelete: Array<out PsiElement>): MutableCollection<String>? {
        if (element is KtNamedFunction || element is KtProperty) {
            val jetClass = element.getNonStrictParentOfType<KtClass>()
            if (jetClass == null || jetClass.getBody() != element.parent) return null

            val modifierList = jetClass.modifierList
            if (modifierList != null && modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return null

            val bindingContext = (element as KtElement).analyze()

            val declarationDescriptor =
                    bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? CallableMemberDescriptor ?: return null

            return declarationDescriptor.overriddenDescriptors
                    .asSequence()
                    .filter { overridenDescriptor -> overridenDescriptor.modality == Modality.ABSTRACT }
                    .mapTo(ArrayList()) { overridenDescriptor ->
                        KotlinBundle.message(
                                "x.implements.y",
                                formatFunction(declarationDescriptor, true),
                                formatClass(declarationDescriptor.containingDeclaration, true),
                                formatFunction(overridenDescriptor, true),
                                formatClass(overridenDescriptor.containingDeclaration, true)
                        )
                    }
        }

        return super.findConflicts(element, allElementsToDelete)
    }

    /*
     * Mostly copied from JavaSafeDeleteProcessor.preprocessUsages
     * Revision: d4fc033
     * (replaced original dialog)
     */
    override fun preprocessUsages(project: Project, usages: Array<out UsageInfo>): Array<UsageInfo>? {
        val result = ArrayList<UsageInfo>()
        val overridingMethodUsages = ArrayList<UsageInfo>()

        for (usage in usages) {
            if (usage is KotlinSafeDeleteOverridingUsageInfo) {
                overridingMethodUsages.add(usage)
            } else {
                result.add(usage)
            }
        }

        if (!overridingMethodUsages.isEmpty()) {
            if (ApplicationManager.getApplication()!!.isUnitTestMode) {
                result.addAll(overridingMethodUsages)
            } else {
                val dialog = KotlinOverridingDialog(project, overridingMethodUsages)
                dialog.show()

                if (!dialog.isOK) return null

                result.addAll(dialog.selected)
            }
        }

        return result.toTypedArray()
    }

    override fun prepareForDeletion(element: PsiElement) {
        if (element is KtDeclaration) {
            element.actualsForExpected().forEach {
                if (it is KtParameter) {
                    (it.parent as? KtParameterList)?.removeParameter(it)
                }
                else {
                    it.removeModifier(KtTokens.IMPL_KEYWORD)
                    it.removeModifier(KtTokens.ACTUAL_KEYWORD)
                }
            }
        }

        when (element) {
            is PsiMethod -> element.cleanUpOverrides()

            is KtNamedFunction ->
                if (!element.isLocal) {
                    element.getRepresentativeLightMethod()?.cleanUpOverrides()
                }

            is KtProperty ->
                if (!element.isLocal) {
                    element.toLightMethods().forEach(PsiMethod::cleanUpOverrides)
                }

            is KtTypeParameter ->
                element.deleteElementAndCleanParent()

            is KtParameter -> {
                element.ownerFunction?.let {
                    if (it.dropActualModifier == true) {
                        it.removeModifier(KtTokens.IMPL_KEYWORD)
                        it.removeModifier(KtTokens.ACTUAL_KEYWORD)
                        it.dropActualModifier = null
                    }
                }
                (element.parent as KtParameterList).removeParameter(element)
            }
        }
    }

    private fun shouldAllowPropagationToExpected(parameter: KtParameter): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) return parameter.project.ALLOW_LIFTING_ACTUAL_PARAMETER_TO_EXPECTED

        return Messages.showYesNoDialog(
                "Do you want to delete this parameter in expected declaration and all related actual ones?",
                RefactoringBundle.message("safe.delete.title"),
                Messages.getQuestionIcon()
        ) == Messages.YES
    }

    override fun getElementsToSearch(
            element: PsiElement, module: Module?, allElementsToDelete: Collection<PsiElement>
    ): Collection<PsiElement>? {
        when (element) {
            is KtParameter -> {
                val headerParameter = element.liftToExpected() as? KtParameter
                if (headerParameter != null && headerParameter != element) {
                    if (shouldAllowPropagationToExpected(element)) {
                        return listOf(headerParameter)
                    } else {
                        element.ownerFunction?.dropActualModifier = true
                        return listOf(element)
                    }
                }

                return element.toPsiParameters().flatMap { psiParameter ->
                    checkParametersInMethodHierarchy(psiParameter) ?: emptyList()
                }.ifEmpty { listOf(element) }
            }

            is PsiParameter ->
                return checkParametersInMethodHierarchy(element)
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode) return Collections.singletonList(element)

        return when (element) {
            is KtNamedFunction, is KtProperty -> checkSuperMethods(element as KtDeclaration, allElementsToDelete, "delete (with usage search)")
            else -> super.getElementsToSearch(element, module, allElementsToDelete)
        }
    }
}
