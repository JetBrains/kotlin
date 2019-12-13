/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class InfixCallToOrdinaryIntention :
    SelfTargetingIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Replace infix call with ordinary call") {
    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken != KtTokens.IDENTIFIER || element.left == null || element.right == null) return false
        return element.operationReference.textRange.containsOffset(caretOffset)
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        convert(element)
    }

    companion object {
        fun convert(element: KtBinaryExpression): KtExpression {
            val argument = KtPsiUtil.safeDeparenthesize(element.right!!)
            val pattern = "$0.$1" + when (argument) {
                is KtLambdaExpression -> " $2:'{}'"
                else -> "($2)"
            }
            val replacement =
                KtPsiFactory(element).createExpressionByPattern(pattern, element.left!!, element.operationReference.text, argument)
            return element.replace(replacement) as KtExpression
        }
    }
}
