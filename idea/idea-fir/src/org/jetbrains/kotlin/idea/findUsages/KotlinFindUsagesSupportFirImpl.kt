/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.*

class KotlinFindUsagesSupportFirImpl : KotlinFindUsagesSupport {
    override fun isCallReceiverRefersToCompanionObject(element: KtElement, companionObject: KtObjectDeclaration): Boolean {
        return false
    }

    override fun isDataClassComponentFunction(element: KtParameter): Boolean {
        return false
    }

    override fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement> {
        return emptyList()
    }

    override fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String? {
        return (declaration as? KtNamedDeclaration)?.name ?: "SUPPORT FOR FIR"
    }

    override fun isConstructorUsage(psiReference: PsiReference, ktClassOrObject: KtClassOrObject): Boolean {
        return false
    }

    override fun checkSuperMethods(declaration: KtDeclaration, ignore: Collection<PsiElement>?, actionString: String): List<PsiElement> {
        return listOf(declaration)
    }

    override fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope {
        return delegate
    }
}