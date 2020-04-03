/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.util.PsiPrecedences
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.types.expressions.OperatorConventions

class SwapBinaryExpressionIntention : SelfTargetingIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    KotlinBundle.lazyMessage("flip.binary.expression")
), LowPriorityAction {
    companion object {
        private val SUPPORTED_OPERATIONS: Set<KtSingleValueToken> by lazy {
            setOf(PLUS, MUL, OROR, ANDAND, EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ, GT, LT, GTEQ, LTEQ)
        }

        private val SUPPORTED_OPERATION_NAMES: Set<String> by lazy {
            SUPPORTED_OPERATIONS.asSequence().mapNotNull { OperatorConventions.BINARY_OPERATION_NAMES[it]?.asString() }.toSet() +
                    setOf("xor", "or", "and", "equals")
        }
    }

    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        val opRef = element.operationReference
        if (!opRef.textRange.containsOffset(caretOffset)) return false

        if (leftSubject(element) == null || rightSubject(element) == null) {
            return false
        }

        val operationToken = element.operationToken
        val operationTokenText = opRef.text
        if (operationToken in SUPPORTED_OPERATIONS
            || operationToken == IDENTIFIER && operationTokenText in SUPPORTED_OPERATION_NAMES
        ) {
            setTextGetter(KotlinBundle.lazyMessage("flip.0", operationTokenText))
            return true
        }
        return false
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        // Have to use text here to preserve names like "plus"
        val convertedOperator = when (val operator = element.operationReference.text!!) {
            ">" -> "<"
            "<" -> ">"
            "<=" -> ">="
            ">=" -> "<="
            else -> operator
        }

        val left = leftSubject(element) ?: return
        val right = rightSubject(element) ?: return
        val rightCopy = right.copied()
        val leftCopy = left.copied()
        left.replace(rightCopy)
        right.replace(leftCopy)
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 $convertedOperator $1", element.left!!, element.right!!))
    }

    private fun leftSubject(element: KtBinaryExpression): KtExpression? =
        firstDescendantOfTighterPrecedence(element.left, PsiPrecedences.getPrecedence(element), KtBinaryExpression::getRight)

    private fun rightSubject(element: KtBinaryExpression): KtExpression? =
        firstDescendantOfTighterPrecedence(element.right, PsiPrecedences.getPrecedence(element), KtBinaryExpression::getLeft)

    private fun firstDescendantOfTighterPrecedence(
        expression: KtExpression?,
        precedence: Int,
        getChild: KtBinaryExpression.() -> KtExpression?
    ): KtExpression? {
        if (expression is KtBinaryExpression) {
            val expressionPrecedence = PsiPrecedences.getPrecedence(expression)
            if (!PsiPrecedences.isTighter(expressionPrecedence, precedence)) {
                return firstDescendantOfTighterPrecedence(expression.getChild(), precedence, getChild)
            }
        }

        return expression
    }
}
