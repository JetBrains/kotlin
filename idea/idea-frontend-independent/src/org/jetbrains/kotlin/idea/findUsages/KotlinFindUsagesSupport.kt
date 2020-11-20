/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.psi.*

interface KotlinFindUsagesSupport {

    companion object {
        fun getInstance(project: Project): KotlinFindUsagesSupport {
            return ServiceManager.getService(project, KotlinFindUsagesSupport::class.java)
        }

        val KtParameter.isDataClassComponentFunction: Boolean
            get() =  getInstance(project).isDataClassComponentFunction(this)

        fun KtElement.isCallReceiverRefersToCompanionObject(companionObject: KtObjectDeclaration): Boolean =
            getInstance(project).isCallReceiverRefersToCompanionObject(this, companionObject)

        fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement> =
            getInstance(target.project).getTopMostOverriddenElementsToHighlight(target)

        fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String? =
            getInstance(declaration.project).tryRenderDeclarationCompactStyle(declaration)

        fun PsiReference.isConstructorUsage(ktClassOrObject: KtClassOrObject): Boolean =
            getInstance(ktClassOrObject.project).isConstructorUsage(this, ktClassOrObject)

        fun checkSuperMethods(
            declaration: KtDeclaration,
            ignore: Collection<PsiElement>?,
            @Nls actionString: String
        ): List<PsiElement> = getInstance(declaration.project).checkSuperMethods(declaration, ignore, actionString)

        fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope =
            getInstance(project).sourcesAndLibraries(delegate, project)
    }

    fun isCallReceiverRefersToCompanionObject(element: KtElement, companionObject: KtObjectDeclaration): Boolean

    fun isDataClassComponentFunction(element: KtParameter): Boolean

    fun getTopMostOverriddenElementsToHighlight(target: PsiElement): List<PsiElement>

    fun tryRenderDeclarationCompactStyle(declaration: KtDeclaration): String?

    fun isConstructorUsage(psiReference: PsiReference, ktClassOrObject: KtClassOrObject): Boolean

    fun checkSuperMethods(
        declaration: KtDeclaration,
        ignore: Collection<PsiElement>?,
        @Nls actionString: String
    ): List<PsiElement>

    fun sourcesAndLibraries(delegate: GlobalSearchScope, project: Project): GlobalSearchScope
}