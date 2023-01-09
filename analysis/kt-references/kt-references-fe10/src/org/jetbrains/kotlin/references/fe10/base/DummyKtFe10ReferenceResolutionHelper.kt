/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10.base

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * A dummy implementation of [KtFe10ReferenceResolutionHelper]
 *
 * The actual implementation in IDE, [KtFe10ReferenceResolutionHelperImpl], is not always available, e.g., in Android Lint CLI.
 * Other CLI clients that use reference resolution via PSI utils may need to register this dummy implementation too.
 */
@Suppress("Unused")
object DummyKtFe10ReferenceResolutionHelper : KtFe10ReferenceResolutionHelper {
    override fun isInProjectOrLibSource(element: PsiElement, includeScriptsOutsideSourceRoots: Boolean): Boolean = false

    override fun resolveImportReference(file: KtFile, fqName: FqName): Collection<DeclarationDescriptor> = emptyList()

    override fun partialAnalyze(element: KtElement): BindingContext = BindingContext.EMPTY

    override fun findDecompiledDeclaration(
        project: Project,
        referencedDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope?
    ): KtDeclaration? = null

    override fun findPsiDeclarations(
        declaration: DeclarationDescriptor,
        project: Project,
        resolveScope: GlobalSearchScope
    ): Collection<PsiElement> = emptyList()

    override fun resolveKDocLink(element: KDocName): Collection<DeclarationDescriptor> = emptyList()
}