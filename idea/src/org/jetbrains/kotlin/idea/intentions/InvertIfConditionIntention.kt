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
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*

public class InvertIfConditionIntention : JetSelfTargetingIntention<JetIfExpression>(javaClass(), "Invert 'if' condition") {
    override fun isApplicableTo(element: JetIfExpression, caretOffset: Int): Boolean {
        if (!element.getIfKeyword().getTextRange().containsOffset(caretOffset)) return false
        return element.getCondition() != null && element.getThen() != null
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val psiFactory = JetPsiFactory(element)

        val condition = element.getCondition()!!
        val newCondition = negate(condition)

        val thenBranch = element.getThen()
        val elseBranch = element.getElse() ?: psiFactory.createEmptyBody()

        val newThen = if (elseBranch is JetIfExpression)
            psiFactory.wrapInABlock(elseBranch)
        else
            elseBranch

        val newElse = if (thenBranch is JetBlockExpression && thenBranch.getStatements().isEmpty())
            null
        else
            thenBranch

        element.replace(psiFactory.createIf(newCondition, newThen, newElse))
    }

    companion object {
        private val NEGATABLE_OPERATORS = setOf(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ,
                JetTokens.EXCLEQEQEQ, JetTokens.IS_KEYWORD, JetTokens.NOT_IS, JetTokens.IN_KEYWORD,
                JetTokens.NOT_IN, JetTokens.LT, JetTokens.LTEQ, JetTokens.GT, JetTokens.GTEQ)

        private fun getNegatedOperatorText(token: IElementType): String {
            return when(token) {
                JetTokens.EQEQ -> JetTokens.EXCLEQ.getValue()
                JetTokens.EXCLEQ -> JetTokens.EQEQ.getValue()
                JetTokens.EQEQEQ -> JetTokens.EXCLEQEQEQ.getValue()
                JetTokens.EXCLEQEQEQ -> JetTokens.EQEQEQ.getValue()
                JetTokens.IS_KEYWORD -> JetTokens.NOT_IS.getValue()
                JetTokens.NOT_IS -> JetTokens.IS_KEYWORD.getValue()
                JetTokens.IN_KEYWORD -> JetTokens.NOT_IN.getValue()
                JetTokens.NOT_IN -> JetTokens.IN_KEYWORD.getValue()
                JetTokens.LT -> JetTokens.GTEQ.getValue()
                JetTokens.LTEQ -> JetTokens.GT.getValue()
                JetTokens.GT -> JetTokens.LTEQ.getValue()
                JetTokens.GTEQ -> JetTokens.LT.getValue()
                else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
            }
        }

        private fun negate(expression: JetExpression): JetExpression {
            val specialNegation = specialNegationText(expression)
            if (specialNegation != null) return specialNegation

            val negationExpr = JetPsiFactory(expression).createExpression("!a") as JetPrefixExpression
            negationExpr.getBaseExpression()!!.replace(expression)
            return negationExpr
        }

        private fun specialNegationText(expression: JetExpression): JetExpression? {
            val factory = JetPsiFactory(expression)
            when (expression) {
                is JetPrefixExpression -> {
                    if (expression.getOperationReference().getReferencedName() == "!") {
                        val baseExpression = expression.getBaseExpression()
                        if (baseExpression != null) {
                            return JetPsiUtil.safeDeparenthesize(baseExpression)
                        }
                    }
                }

                is JetBinaryExpression -> {
                    val operator = expression.getOperationToken() ?: return null
                    if (operator !in NEGATABLE_OPERATORS) return null
                    val left = expression.getLeft() ?: return null
                    val right = expression.getRight() ?: return null
                    return factory.createExpression(left.getText() + " " + getNegatedOperatorText(operator) + " " + right.getText())
                }

                is JetConstantExpression -> {
                    return when (expression.getText()) {
                        "true" -> factory.createExpression("false")
                        "false" -> factory.createExpression("true")
                        else -> null
                    }
                }
            }
            return null
        }
    }
}
