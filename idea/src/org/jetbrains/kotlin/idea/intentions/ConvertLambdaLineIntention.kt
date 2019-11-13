/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.allChildren

sealed class ConvertLambdaLineIntention(private val toMultiLine: Boolean) : SelfTargetingIntention<KtLambdaExpression>(
    KtLambdaExpression::class.java, "Convert to ${if (toMultiLine) "multi" else "single"}-line"
) {
    override fun isApplicableTo(element: KtLambdaExpression, caretOffset: Int): Boolean {
        val functionLiteral = element.functionLiteral
        val body = functionLiteral.bodyBlockExpression ?: return false
        val startLine = functionLiteral.getLineNumber(start = true)
        val endLine = functionLiteral.getLineNumber(start = false)
        return toMultiLine && startLine == endLine
                || !toMultiLine && startLine != endLine && body.statements.size <= 1 && body.allChildren.none { it is PsiComment }
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val functionLiteral = element.functionLiteral
        val body = functionLiteral.bodyBlockExpression?.text ?: ""
        val startLineBreak = if (toMultiLine) "\n" else ""
        val endLineBreak = if (toMultiLine && body != "") "\n" else ""
        element.replace(
            KtPsiFactory(element).createLambdaExpression(
                functionLiteral.valueParameters.joinToString { it.text },
                "$startLineBreak$body$endLineBreak"
            )
        )
    }
}

class ConvertLambdaToMultiLineIntention : ConvertLambdaLineIntention(toMultiLine = true)

class ConvertLambdaToSingleLineIntention : ConvertLambdaLineIntention(toMultiLine = false)
