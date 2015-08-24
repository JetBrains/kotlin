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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Conditions
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.safeDelete.ImportSearcher
import com.intellij.refactoring.safeDelete.JavaSafeDeleteProcessor
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.usageInfo.*
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.deleteElementAndCleanParent
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringUtil
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.search.usagesSearch.processDelegationCallConstructorUsages
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
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
                    if (t is JetFile) return false
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
                            if (usageElement.getNonStrictParentOfType<JetValueArgumentName>() != null) null
                            else {
                                usageElement.getNonStrictParentOfType<JetImportDirective>()?.let { importDirective ->
                                    SafeDeleteImportDirectiveUsageInfo(importDirective, element.unwrapped as JetDeclaration)
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

        fun findUsagesByJavaProcessor(jetDeclaration: JetDeclaration): NonCodeUsageSearchInfo {
            return NonCodeUsageSearchInfo(
                    findUsagesByJavaProcessor(
                            jetDeclaration.toLightElements().asSequence(),
                            getIgnoranceCondition()
                    ),
                    jetDeclaration
            )
        }

        fun findKotlinDeclarationUsages(declaration: JetDeclaration): NonCodeUsageSearchInfo {
            ReferencesSearch.search(declaration, declaration.getUseScope())
                    .asSequence()
                    .filterNot { reference -> getIgnoranceCondition().value(reference.getElement()) }
                    .mapTo(usages) { reference ->
                        reference.getElement().getNonStrictParentOfType<JetImportDirective>()?.let { importDirective ->
                            SafeDeleteImportDirectiveUsageInfo(importDirective, element.unwrapped as JetDeclaration)
                        } ?: SafeDeleteReferenceSimpleDeleteUsageInfo(element, declaration, false)
                    }

            return getSearchInfo(declaration)
        }

        fun findTypeParameterUsages(parameter: JetTypeParameter) {
            val owner = parameter.getNonStrictParentOfType<JetTypeParameterListOwner>()
            if (owner == null) return

            val parameterList = owner.getTypeParameters()
            val parameterIndex = parameterList.indexOf(parameter)

            for (reference in ReferencesSearch.search(owner)) {
                if (reference !is JetReference) continue

                val referencedElement = reference.getElement()

                val argList = referencedElement.getNonStrictParentOfType<JetUserType>()?.let { jetType ->
                    jetType.getTypeArgumentList()
                } ?: referencedElement.getNonStrictParentOfType<JetCallExpression>()?.let { callExpression ->
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

        fun findUsagesByCopiedJavaProcessor(element: PsiElement): NonCodeUsageSearchInfo {
            if (element is PsiClass) {
                ClassSearchCopiedFromJava.findClassUsages(element, allElementsToDelete, usages)
                if (element is PsiTypeParameter) {
                    ClassSearchCopiedFromJava.findTypeParameterExternalUsages(element, usages)
                }
            }
            return NonCodeUsageSearchInfo(JavaSafeDeleteProcessor.getUsageInsideDeletedFilter(allElementsToDelete), element)
        }

        return when (element) {
            is JetClassOrObject -> {
                element.toLightClass()?.let { klass ->
                    findDelegationCallUsages(klass)
                    findUsagesByCopiedJavaProcessor(klass)
                }
            }

            is JetSecondaryConstructor -> {
                element.getRepresentativeLightMethod()?.let { method ->
                    findDelegationCallUsages(method)
                    findUsagesByJavaProcessor(method, false)
                }
            }

            is JetNamedFunction -> {
                if (element.isLocal()) {
                    findKotlinDeclarationUsages(element)
                }
                else {
                    element.getRepresentativeLightMethod()?.let { method -> findUsagesByJavaProcessor(method, false) }
                }
            }

            is PsiMethod ->
                findUsagesByJavaProcessor(element, false)

            is JetProperty -> {
                if (element.isLocal()) {
                    findKotlinDeclarationUsages(element)
                }
                else {
                    findUsagesByJavaProcessor(element)
                }
            }

            is JetTypeParameter -> {
                findTypeParameterUsages(element)
                element.toLightElements().forEach { findUsagesByCopiedJavaProcessor(it) }
                getSearchInfo(element)
            }

            is JetParameter ->
                findUsagesByJavaProcessor(element)

            else -> null
        } ?: getSearchInfo(element)
    }

    override fun findConflicts(element: PsiElement, allElementsToDelete: Array<out PsiElement>): MutableCollection<String>? {
        if (element is JetNamedFunction || element is JetProperty) {
            val jetClass = element.getNonStrictParentOfType<JetClass>()
            if (jetClass == null || jetClass.getBody() != element.getParent()) return null

            val modifierList = jetClass.getModifierList()
            if (modifierList != null && modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return null

            val bindingContext = (element as JetElement).analyze()

            val declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            if (declarationDescriptor !is CallableMemberDescriptor) return null

            return declarationDescriptor.getOverriddenDescriptors()
                    .asSequence()
                    .filter { overridenDescriptor -> overridenDescriptor.getModality() == Modality.ABSTRACT }
                    .mapTo(ArrayList<String>()) { overridenDescriptor ->
                        JetBundle.message(
                                "x.implements.y",
                                JetRefactoringUtil.formatFunction(declarationDescriptor, true),
                                JetRefactoringUtil.formatClass(declarationDescriptor.getContainingDeclaration(), true),
                                JetRefactoringUtil.formatFunction(overridenDescriptor, true),
                                JetRefactoringUtil.formatClass(overridenDescriptor.getContainingDeclaration(), true)
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

            is JetNamedFunction ->
                if (!element.isLocal()) {
                    element.getRepresentativeLightMethod()?.cleanUpOverrides()
                }

            is JetProperty ->
                if (!element.isLocal()) {
                    element.toLightMethods().forEach { method -> method.cleanUpOverrides() }
                }

            is JetTypeParameter ->
                element.deleteElementAndCleanParent()

            is JetParameter ->
                (element.getParent() as JetParameterList).removeParameter(element)
        }
    }

    override fun getElementsToSearch(
            element: PsiElement, module: Module?, allElementsToDelete: Collection<PsiElement>
    ): Collection<PsiElement>? {
        when (element) {
            is JetParameter ->
                return element.toPsiParameter()?.let { psiParameter ->
                    JetRefactoringUtil.checkParametersInMethodHierarchy(psiParameter)
                } ?: Collections.singletonList(element)

            is PsiParameter ->
                return JetRefactoringUtil.checkParametersInMethodHierarchy(element)
        }

        if (ApplicationManager.getApplication()!!.isUnitTestMode()) return Collections.singletonList(element)

        return when (element) {
            is JetNamedFunction, is JetProperty ->
                JetRefactoringUtil.checkSuperMethods(
                        element as JetDeclaration, allElementsToDelete, "super.methods.delete.with.usage.search"
                )

            else -> super.getElementsToSearch(element, module, allElementsToDelete)
        }
    }

    // Mostly copied from JavaSafeDeleteProcessor.findClassUsages. Temporary solution
    private object ClassSearchCopiedFromJava {
        private val LOG = Logger.getInstance("#" + javaClass<KotlinSafeDeleteProcessor>().canonicalName)

        fun findClassUsages(psiClass: PsiClass, allElementsToDelete: Array<out PsiElement>, usages: MutableList<UsageInfo>) {
            val justPrivates = containsOnlyPrivates(psiClass)

            ReferencesSearch.search(psiClass).forEach(object : Processor<PsiReference> {
                override fun process(reference: PsiReference): Boolean {
                    val element = reference.element

                    fun isInside(place: PsiElement, ancestors: Array<out PsiElement>): Boolean {
                        for (ancestor in ancestors) {
                            if (JavaSafeDeleteProcessor.isInside(place, ancestor)) return true
                        }
                        return false
                    }

                    fun isInNonStaticImport(element: PsiElement): Boolean {
                        return ImportSearcher.getImport(element, true) != null
                    }

                    if (!isInside(element, allElementsToDelete)) {
                        val parent = element.parent
                        if (parent is PsiReferenceList) {
                            val pparent = parent.parent
                            if (pparent is PsiClass) {
                                /* If psiClass contains only private members, then it is safe to remove it
                                   and change inheritor's extends/implements accordingly */
                                if (justPrivates && element is PsiJavaCodeReferenceElement) {
                                    if (parent == pparent.extendsList || parent == pparent.implementsList) {
                                        usages.add(SafeDeleteExtendsClassUsageInfo(element, psiClass, pparent))
                                        return true
                                    }
                                }
                            }
                        }
                        LOG.assertTrue(element.textRange != null)

                        val importDirective = getImportDirective(element, element)
                        if (importDirective != null) {
                            usages.add(SafeDeleteReferenceJavaDeleteUsageInfo(importDirective, psiClass, true))
                        }
                        else {
                            val containingFile = psiClass.containingFile
                            val sameFileWithSingleClass = containingFile is PsiClassOwner
                                                          && containingFile.classes.size() == 1
                                                          && element.containingFile === containingFile

                            usages.add(SafeDeleteReferenceJavaDeleteUsageInfo(element, psiClass,
                                    sameFileWithSingleClass || isInNonStaticImport(element)))
                        }
                    }
                    return true
                }
            })
        }

        fun findTypeParameterExternalUsages(typeParameter: PsiTypeParameter, usages: MutableCollection<UsageInfo>) {
            val owner = typeParameter.owner
            if (owner != null) {
                val parameterList = owner.typeParameterList
                if (parameterList != null) {
                    val paramsCount = parameterList.typeParameters.size()
                    val index = parameterList.getTypeParameterIndex(typeParameter)

                    ReferencesSearch.search(owner).forEach(object : Processor<PsiReference> {
                        override fun process(reference: PsiReference): Boolean {
                            if (reference is PsiJavaCodeReferenceElement) {
                                val parameterList = reference.parameterList
                                if (parameterList != null) {
                                    val typeArgs = parameterList.typeParameterElements
                                    if (typeArgs.size() > index) {
                                        if (typeArgs.size() == 1 && paramsCount > 1 && typeArgs[0].type is PsiDiamondType) return true
                                        usages.add(SafeDeleteReferenceJavaDeleteUsageInfo(typeArgs[index], typeParameter, true))
                                    }
                                }
                            }
                            return true
                        }
                    })
                }
            }
        }

        private fun getImportDirective(element: PsiElement?, original: PsiElement): JetImportDirective? = when (element) {
            is JetDotQualifiedExpression -> getImportDirective(element.parent, original)
            is JetNameReferenceExpression -> getImportDirective(element.parent, original)
            is JetImportDirective -> {
                val lastChild = element.importedReference
                if (lastChild == original || (lastChild is JetDotQualifiedExpression && lastChild.selectorExpression == original)) {
                    element
                } else {
                    null
                }
            }
            else -> null
        }

        private fun containsOnlyPrivates(aClass: PsiClass): Boolean {
            val fields = aClass.fields
            for (field in fields) {
                if (!field.hasModifierProperty(PsiModifier.PRIVATE)) return false
            }

            val methods = aClass.methods
            for (method in methods) {
                if (!method.hasModifierProperty(PsiModifier.PRIVATE)) {
                    if (method.isConstructor) {
                        //skip non-private constructors with call to super only
                        val body = method.body
                        if (body != null) {
                            val statements = body.statements
                            if (statements.size() == 0) continue
                            if (statements.size() == 1 && statements[0] is PsiExpressionStatement) {
                                val expression = (statements[0] as PsiExpressionStatement).expression
                                if (expression is PsiMethodCallExpression) {
                                    val methodExpression = expression.methodExpression
                                    if (methodExpression.text == PsiKeyword.SUPER) {
                                        continue
                                    }
                                }
                            }
                        }
                    }
                    return false
                }
            }

            val inners = aClass.innerClasses
            for (inner in inners) {
                if (!inner.hasModifierProperty(PsiModifier.PRIVATE)) return false
            }

            return true
        }
    }
}
