/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.isConstructorUsage
import org.jetbrains.kotlin.psi.*

class KotlinFindUsagesSupportImpl : KotlinFindUsagesSupport {
    override fun isCallReceiverRefersToCompanionObject(element: KtElement, companionObject: KtObjectDeclaration): Boolean =
        org.jetbrains.kotlin.idea.search.usagesSearch.isCallReceiverRefersToCompanionObject(element, companionObject)

    override fun isDataClassComponentFunction(element: KtParameter): Boolean =
        element.dataClassComponentFunction() != null

    override fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement> =
        org.jetbrains.kotlin.idea.search.usagesSearch.getTopMostOverriddenElementsToHighlight(target)

    override fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String? =
        org.jetbrains.kotlin.idea.search.usagesSearch.tryRenderDeclarationCompactStyle(declaration)

    override fun isConstructorUsage(psiReference: PsiReference, ktClassOrObject: KtClassOrObject): Boolean =
        psiReference.isConstructorUsage(ktClassOrObject)

    override fun checkSuperMethods(declaration: KtDeclaration, ignore: Collection<PsiElement>?, actionString: String): List<PsiElement> =
        org.jetbrains.kotlin.idea.refactoring.checkSuperMethods(declaration, ignore, actionString)

    override fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope =
        org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope.sourcesAndLibraries(delegate, project)
}