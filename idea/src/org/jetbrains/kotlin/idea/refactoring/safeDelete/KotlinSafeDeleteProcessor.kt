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
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverrideAnnotation
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteOverridingMethodUsageInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceJavaDeleteUsageInfo
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.deleteElementAndCleanParent
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

public class KotlinSafeDeleteProcessor : JavaSafeDeleteProcessor() {
    override fun handlesElement(element: PsiElement): Boolean = element.canDeleteElement()

    override fun findUsages(
            element: PsiElement, allElementsToDelete: Array<out PsiElement>, usages: MutableList<UsageInfo>
    ): NonCodeUsageSearchInfo {
        val deleteList = allElementsToDelete.toList()

        fun getIgnoranceCondition(): Condition<PsiElement> {
            return object : Condition<PsiElement> {
                override fun value(t: PsiElement?): Boolean {
                    if (t is KtFile) return false
                    return deleteList.any { element -> JavaSafeDeleteProcessor.isInside(t, element.unwrapped) }
                }
            }
        }

        fun getSearchInfo(element: PsiElement): NonCodeUsageSearchInfo {
            return NonCodeUsageSearchInfo(getIgnoranceCondition(), element)
        }

        fun findUsagesByJavaProcessor(element: PsiElement, forceReferencedElementUnwrapping: Boolean): NonCodeUsageSearchInfo? {
            val javaUsages = ArrayList<UsageInfo>()
            val searchInfo = super.findUsages(element, allElementsToDelete, javaUsages)

            javaUsages.map { usageInfo ->
                when (usageInfo) {
                    is SafeDeleteOverridingMethodUsageInfo ->
                        usageInfo.getSmartPointer().getElement()?.let { usageElement ->
                            KotlinSafeDeleteOverridingUsageInfo(usageElement, usageInfo.getReferencedElement())
                        }

                    is SafeDeleteOverrideAnnotation ->
                        usageInfo.getSmartPointer().getElement()?.let { usageElement ->
                            if (usageElement.toLightMethods().all { method -> method.findSuperMethods().size() == 0 }) {
                                KotlinSafeDeleteOverrideAnnotation(usageElement, usageInfo.getReferencedElement())
                            }
                            else null
                        }

                    is SafeDeleteReferenceJavaDeleteUsageInfo ->
                        usageInfo.getElement()?.let { usageElement ->
                            if (usageElement.getNonStrictParentOfType<KtValueArgumentName>() != null) null
                            else {
                                usageElement.getNonStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                                    SafeDeleteImportDirectiveUsageInfo(importDirective, element.unwrapped as KtDeclaration)
                                } ?: if (forceReferencedElementUnwrapping) {
                                    SafeDeleteReferenceJavaDeleteUsageInfo(usageElement, element.unwrapped, usageInfo.isSafeDelete())
                                } else usageInfo
                            }
                        }

                    else -> usageInfo
                }
            }.filterNotNull().toCollection(usages)

            return searchInfo
        }

        fun findUsagesByJavaProcessor(elements: Sequence<PsiElement>, insideDeleted: Condition<PsiElement>): Condition<PsiElement> =
                elements
                        .map { element -> findUsagesByJavaProcessor(element, true)?.getInsideDeletedCondition() }
                        .filterNotNull()
                        .fold(insideDeleted) { condition1, condition2 -> Conditions.or(condition1, condition2) }

        fun findUsagesByJavaProcessor(ktDeclaration: KtDeclaration): NonCodeUsageSearchInfo {
            return NonCodeUsageSearchInfo(
                    findUsagesByJavaProcessor(
                            ktDeclaration.toLightElements().asSequence(),
                            getIgnoranceCondition()
                    ),
                    ktDeclaration
            )
        }

        fun findKotlinDeclarationUsages(declaration: KtDeclaration): NonCodeUsageSearchInfo {
            ReferencesSearch.search(declaration, declaration.getUseScope())
                    .asSequence()
                    .filterNot { reference -> getIgnoranceCondition().value(reference.getElement()) }
                    .mapTo(usages) { reference ->
                        reference.getElement().getNonStrictParentOfType<KtImportDirective>()?.let { importDirective ->
                            SafeDeleteImportDirectiveUsageInfo(importDirective, element.unwrapped as KtDeclaration)
                        } ?: SafeDeleteReferenceSimpleDeleteUsageInfo(element, declaration, false)
                    }

            return getSearchInfo(declaration)
        }

        fun findTypeParameterUsages(parameter: KtTypeParameter) {
            val owner = parameter.getNonStrictParentOfType<KtTypeParameterListOwner>()
            if (owner == null) return

            val parameterList = owner.getTypeParameters()
            val parameterIndex = parameterList.indexOf(parameter)

            for (reference in ReferencesSearch.search(owner)) {
                if (reference !is KtReference) continue

                val referencedElement = reference.getElement()

                val argList = referencedElement.getNonStrictParentOfType<KtUserType>()?.let { jetType ->
                    jetType.getTypeArgumentList()
                } ?: referencedElement.getNonStrictParentOfType<KtCallExpression>()?.let { callExpression ->
                    callExpression.getTypeArgumentList()
                } ?: null

                if (argList != null) {
                    val projections = argList.getArguments()
                    if (parameterIndex < projections.size()) {
                        usages.add(SafeDeleteTypeArgumentListUsageInfo(projections.get(parameterIndex), parameter))
                    }
                }
            }
        }

        fun findDelegationCallUsages(element: PsiElement) {
            element.processDelegationCallConstructorUsages(element.getUseScope()) {
                usages.add(SafeDeleteReferenceSimpleDeleteUsageInfo(it, element, false))
            }
        }

        return when (element) {
            is KtClassOrObject -> {
                element.toLightClass()?.let { klass ->
                    findDelegationCallUsages(klass)
                    findUsagesByJavaProcessor(klass, false)
                }
            }

            is KtSecondaryConstructor -> {
                element.getRepresentativeLightMethod()?.let { method ->
                    findDelegationCallUsages(method)
                    findUsagesByJavaProcessor(method, false)
                }
            }

            is KtNamedFunction -> {
                if (!LightClassUtil.canGenerateLightClass(element)) {
                    findKotlinDeclarationUsages(element)
                }
                else {
                    element.toLightMethods().map { method -> findUsagesByJavaProcessor(method, false) }.firstOrNull()
                }
            }

            is PsiMethod ->
                findUsagesByJavaProcessor(element, false)

            is KtProperty -> {
                if (!LightClassUtil.canGenerateLightClass(element)) {
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

            else -> null
        } ?: getSearchInfo(element)
    }

    override fun findConflicts(element: PsiElement, allElementsToDelete: Array<out PsiElement>): MutableCollection<String>? {
        if (element is KtNamedFunction || element is KtProperty) {
            val jetClass = element.getNonStrictParentOfType<KtClass>()
            if (jetClass == null || jetClass.getBody() != element.getParent()) return null

            val modifierList = jetClass.getModifierList()
            if (modifierList != null && modifierList.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return null

            val bindingContext = (element as KtElement).analyze()

            val declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            if (declarationDescriptor !is CallableMemberDescriptor) return null

            return declarationDescriptor.getOverriddenDescriptors()
                    .asSequence()
                    .filter { overridenDescriptor -> overridenDescriptor.getModality() == Modality.ABSTRACT }
                    .mapTo(ArrayList<String>()) { overridenDescriptor ->
                        KotlinBundle.message(
                                "x.implements.y",
                                KotlinRefactoringUtil.formatFunction(declarationDescriptor, true),
                                KotlinRefactoringUtil.formatClass(declarationDescriptor.getContainingDeclaration(), true),
                                KotlinRefactoringUtil.formatFunction(overridenDescriptor, true),
                                KotlinRefactoringUtil.formatClass(overridenDescriptor.getContainingDeclaration(), true)
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
            if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
                result.addAll(overridingMethodUsages)
            } else {
                val dialog = KotlinOverridingDialog(project, overridingMethodUsages)
                dialog.show()

                if (!dialog.isOK()) return null

                result.addAll(dialog.getSelected())
            }
        }

        return result.toTypedArray()
    }

    override fun prepareForDeletion(element: PsiElement) {
        when (element) {
            is PsiMethod -> element.cleanUpOverrides()

            is KtNamedFunction ->
                if (!element.isLocal()) {
                    element.getRepresentativeLightMethod()?.cleanUpOverrides()
                }

            is KtProperty ->
                if (!element.isLocal()) {
                    element.toLightMethods().forEach { method -> method.cleanUpOverrides() }
                }

            is KtTypeParameter ->
                element.deleteElementAndCleanParent()

            is KtParameter ->
                (element.getParent() as KtParameterList).removeParameter(element)
        }
    }

    override fun getElementsToSearch(
            element: PsiElement, module: Module?, allElementsToDelete: Collection<PsiElement>
    ): Collection<PsiElement>? {
        when (element) {
            is KtParameter ->
                return element.toPsiParameters().flatMap { psiParameter ->
                    KotlinRefactoringUtil.checkParametersInMethodHierarchy(psiParameter) ?: emptyList()
                }.ifEmpty { listOf(element) }

            is PsiParameter ->
                return KotlinRefactoringUtil.checkParametersInMethodHierarchy(element)
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode()) return Collections.singletonList(element)

        return when (element) {
            is KtNamedFunction, is KtProperty ->
                KotlinRefactoringUtil.checkSuperMethods(
                        element as KtDeclaration, allElementsToDelete, "super.methods.delete.with.usage.search"
                )

            else -> super.getElementsToSearch(element, module, allElementsToDelete)
        }
    }
}
