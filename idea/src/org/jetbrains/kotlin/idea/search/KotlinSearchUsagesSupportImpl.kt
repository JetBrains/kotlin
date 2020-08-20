/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.idea.core.isInheritable
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasShortNameIndex
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.expectedDeclarationIfAny
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.ImportPath

class KotlinSearchUsagesSupportImpl : KotlinSearchUsagesSupport {

    override fun dataClassComponentMethodName(element: KtParameter): String? =
        element.dataClassComponentFunction()?.name?.asString()

    override fun hasType(element: KtExpression): Boolean =
        org.jetbrains.kotlin.idea.search.usagesSearch.hasType(element)

    override fun isSamInterface(psiClass: PsiClass): Boolean =
        org.jetbrains.kotlin.idea.search.usagesSearch.isSamInterface(psiClass)

    override fun <T : PsiNamedElement> filterDataClassComponentsIfDisabled(
        elements: List<T>,
        kotlinOptions: KotlinReferencesSearchOptions
    ): List<T> =
        elements.filterDataClassComponentsIfDisabled(kotlinOptions)

    override fun isCallableOverrideUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean =
        reference.isCallableOverrideUsage(declaration)

    override fun isUsageInContainingDeclaration(reference: PsiReference, declaration: KtNamedDeclaration): Boolean =
        reference.isUsageInContainingDeclaration(declaration)

    override fun isExtensionOfDeclarationClassUsage(reference: PsiReference, declaration: KtNamedDeclaration): Boolean =
        reference.isExtensionOfDeclarationClassUsage(declaration)

    override fun getReceiverTypeSearcherInfo(psiElement: PsiElement, isDestructionDeclarationSearch: Boolean): ReceiverTypeSearcherInfo? =
        psiElement.getReceiverTypeSearcherInfo(isDestructionDeclarationSearch)

    override fun forceResolveReferences(file: KtFile, elements: List<KtElement>) =
        file.forceResolveReferences(elements)

    override fun scriptDefinitionExists(file: PsiFile): Boolean =
        file.scriptDefinitionExists()

    override fun getDefaultImports(file: KtFile): List<ImportPath> =
        file.getDefaultImports()

    override fun forEachKotlinOverride(
        ktClass: KtClass,
        members: List<KtNamedDeclaration>,
        scope: SearchScope,
        processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
    ): Boolean =
        org.jetbrains.kotlin.idea.search.declarationsSearch.forEachKotlinOverride(
            ktClass,
            members,
            scope,
            processor
        )

    override fun forEachOverridingMethod(method: PsiMethod, scope: SearchScope, processor: (PsiMethod) -> Boolean): Boolean =
        method.forEachOverridingMethod(scope, processor)

    override fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> =
        org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsNoWrapping(method)

    override fun findTypeAliasByShortName(shortName: String, project: Project, scope: GlobalSearchScope): Collection<KtTypeAlias> =
        KotlinTypeAliasShortNameIndex.getInstance().get(shortName, project, scope)

    override fun isInProjectSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean): Boolean =
        ProjectRootsUtil.isInProjectSource(element, includeScriptsOutsideSourceRoots)

    override fun isOverridable(declaration: KtDeclaration): Boolean =
        declaration.isOverridable()

    override fun isInheritable(ktClass: KtClass): Boolean =
        ktClass.isInheritable()

    override fun formatJavaOrLightMethod(method: PsiMethod): String =
        org.jetbrains.kotlin.idea.refactoring.formatJavaOrLightMethod(method)

    override fun formatClass(classOrObject: KtClassOrObject): String =
        org.jetbrains.kotlin.idea.refactoring.formatClass(classOrObject)

    override fun expectedDeclarationIfAny(declaration: KtDeclaration): KtDeclaration? =
        declaration.expectedDeclarationIfAny()

    override fun isExpectDeclaration(declaration: KtDeclaration): Boolean =
        declaration.isExpectDeclaration()
}