/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.idea.util.JetPsiPrecedences
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.copied
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public class SwapBinaryExpressionIntention : JetSelfTargetingIntention<JetBinaryExpression>(javaClass(), "Flip binary expression") {
    companion object {
        private val SUPPORTED_OPERATIONS = setOf(PLUS, MUL, OROR, ANDAND, EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ, GT, LT, GTEQ, LTEQ)

        private val SUPPORTED_OPERATION_NAMES = SUPPORTED_OPERATIONS.map { OperatorConventions.BINARY_OPERATION_NAMES[it]?.asString() }.toSet().filterNotNull() +
                                        setOf("xor", "or", "and", "equals", "identityEquals")
    }

    override fun isApplicableTo(element: JetBinaryExpression, caretOffset: Int): Boolean {
        val opRef = element.getOperationReference()
        if (!opRef.getTextRange().containsOffset(caretOffset)) return false

        if (leftSubject(element) == null || rightSubject(element) == null) {
            return false
        }

        val operationToken = element.getOperationToken()
        val operationTokenText = opRef.getText()
        if (operationToken in SUPPORTED_OPERATIONS
                || operationToken == IDENTIFIER && operationTokenText in SUPPORTED_OPERATION_NAMES) {
            setText("Flip '$operationTokenText'")
            return true
        }
        return false
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        // Have to use text here to preserve names like "plus"
        val operator = element.getOperationReference().getText()!!
        val convertedOperator = when (operator) {
            ">" -> "<"
            "<" -> ">"
            "<=" -> ">="
            ">=" -> "<="
            else -> operator
        }
        val left = leftSubject(element)!!
        val right = rightSubject(element)!!
        val rightCopy = right.copied()
        val leftCopy = left.copied()
        left.replace(rightCopy)
        right.replace(leftCopy)
        element.replace(JetPsiFactory(element).createExpressionByPattern("$0 $convertedOperator $1" , element.getLeft()!!, element.getRight()!!))
    }

    private fun leftSubject(element: JetBinaryExpression): JetExpression? {
        return firstDescendantOfTighterPrecedence(element.getLeft(), JetPsiPrecedences.getPrecedence(element), JetBinaryExpression::getRight)
    }

    private fun rightSubject(element: JetBinaryExpression): JetExpression? {
        return firstDescendantOfTighterPrecedence(element.getRight(), JetPsiPrecedences.getPrecedence(element), JetBinaryExpression::getLeft)
    }

    private fun firstDescendantOfTighterPrecedence(expression: JetExpression?, precedence: Int, getChild: JetBinaryExpression.() -> JetExpression?): JetExpression? {
        if (expression is JetBinaryExpression) {
            val expressionPrecedence = JetPsiPrecedences.getPrecedence(expression)
            if (!JetPsiPrecedences.isTighter(expressionPrecedence, precedence)) {
                return firstDescendantOfTighterPrecedence(expression.getChild(), precedence, getChild)
            }
        }
        return expression
    }
}
