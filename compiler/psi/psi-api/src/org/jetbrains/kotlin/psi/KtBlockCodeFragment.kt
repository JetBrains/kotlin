/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes

/**
 * A [KtCodeFragment] whose content is a block of statements, as opposed to a single expression.
 *
 * Use this when the snippet may contain several statements or local declarations, for example a multi-line debugger evaluation. Its
 * [content element][getContentElement] is a [KtBlockExpression].
 */
@OptIn(KtImplementationDetail::class)
class KtBlockCodeFragment(
    viewProvider: FileViewProvider,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
    context: PsiElement?
) : KtCodeFragment(viewProvider, imports, KtNodeTypes.BLOCK_CODE_FRAGMENT, context) {

    constructor(
        project: Project,
        name: String,
        text: CharSequence,
        imports: String?,
        context: PsiElement?
    ) : this(
        createFileViewProviderForLightFile(project, name, text),
        imports,
        context,
    )

    override fun getContentElement() = findChildByClass(KtBlockExpression::class.java)
            ?: throw IllegalStateException("Block expression should be parsed for BlockCodeFragment")
}
