/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes

/**
 * A [KtCodeFragment] whose content is a single expression.
 *
 * This is the most common fragment kind, used, for example, to evaluate a debugger watch expression.
 * Its [content element][getContentElement] is a [KtExpression], or `null` if the text could not be parsed as one.
 */
@OptIn(KtImplementationDetail::class)
class KtExpressionCodeFragment(
    project: Project,
    name: String,
    text: CharSequence,
    imports: String?,
    context: PsiElement?
) : KtCodeFragment(project, name, text, imports, KtNodeTypes.EXPRESSION_CODE_FRAGMENT, context) {

    override fun getContentElement() = findChildByClass(KtExpression::class.java)
}
