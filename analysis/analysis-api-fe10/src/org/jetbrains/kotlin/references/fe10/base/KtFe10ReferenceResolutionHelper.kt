/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10.base

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Temporary helper that allows FE1.0 KtReferences to use some IDE functionality.
 */
@KaPlatformInterface
interface KtFe10ReferenceResolutionHelper {
    fun isInProjectOrLibSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean): Boolean

    fun resolveImportReference(file: KtFile, fqName: FqName): Collection<DeclarationDescriptor>

    fun partialAnalyze(element: KtElement): BindingContext

    fun findDecompiledDeclaration(
        project: Project,
        referencedDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope?
    ): KtDeclaration?

    fun findPsiDeclarations(declaration: DeclarationDescriptor, project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement>

    fun resolveKDocLink(element: KDocName): Collection<DeclarationDescriptor>

    companion object {
        fun getInstance() = ApplicationManager.getApplication().getService(KtFe10ReferenceResolutionHelper::class.java)
    }
}