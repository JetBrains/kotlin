/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetIfExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetUnaryExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lexer.JetToken
import com.intellij.psi.tree.IElementType
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lexer.JetSingleValueToken
import org.jetbrains.jet.lexer.JetKeywordToken
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetBlockExpression

public class InvertIfConditionIntention : JetSelfTargetingIntention<JetIfExpression>("invert.if.condition", javaClass()) {
    fun checkForNegation(element: JetUnaryExpression): Boolean {
        return element.getOperationReference().getReferencedName().equals("!")
    }

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        val condition = element.getCondition()
        val thenBranch = element.getThen()

        return condition != null && thenBranch != null && when (condition) {
            is JetUnaryExpression -> {
                when {
                    checkForNegation(condition) -> {
                        val baseExpression = condition.getBaseExpression()

                        when (baseExpression) {
                            is JetParenthesizedExpression -> baseExpression.getExpression() != null
                            else -> condition.getBaseExpression() != null
                        }
                    }
                    else -> true
                }
            }
            is JetBinaryExpression -> {
                condition.getOperationToken() != null && condition.getLeft() != null && condition.getRight() != null
            }
            else -> true
        }
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val psiFactory = JetPsiFactory(element)

        fun isNegatableOperator(token: IElementType): Boolean {
            return token in array(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ, JetTokens.IS_KEYWORD, JetTokens.NOT_IS, JetTokens.IN_KEYWORD, JetTokens.NOT_IN, JetTokens.LT, JetTokens.LTEQ, JetTokens.GT, JetTokens.GTEQ)
        }

        fun getNegatedOperator(token: IElementType): JetToken {
            return when {
                token == JetTokens.EQEQ -> JetTokens.EXCLEQ
                token == JetTokens.EXCLEQ -> JetTokens.EQEQ
                token == JetTokens.EQEQEQ -> JetTokens.EXCLEQEQEQ
                token == JetTokens.EXCLEQEQEQ -> JetTokens.EQEQEQ
                token == JetTokens.IS_KEYWORD -> JetTokens.NOT_IS
                token == JetTokens.NOT_IS -> JetTokens.IS_KEYWORD
                token == JetTokens.IN_KEYWORD -> JetTokens.NOT_IN
                token == JetTokens.NOT_IN -> JetTokens.IN_KEYWORD
                token == JetTokens.LT -> JetTokens.GTEQ
                token == JetTokens.LTEQ -> JetTokens.GT
                token == JetTokens.GT -> JetTokens.LTEQ
                token == JetTokens.GTEQ -> JetTokens.LT
                else -> throw IllegalArgumentException("The token $token does not have a negated equivalent.")
            }
        }

        fun getTokenText(token: JetToken): String {
            return when (token) {
                is JetSingleValueToken -> token.getValue()
                is JetKeywordToken -> token.getValue()
                else -> throw IllegalArgumentException("The token $token does not have an applicable string value.")
            }
        }

        fun negateExpressionText(element: JetExpression): String {
            val negatedParenthesizedExpressionText = "!(${element.getText()})"
            val possibleNewExpression = psiFactory.createExpression(negatedParenthesizedExpressionText) as JetUnaryExpression
            val innerExpression = possibleNewExpression.getBaseExpression() as JetParenthesizedExpression

            return when {
                JetPsiUtil.areParenthesesUseless(innerExpression) -> "!${element.getText()}"
                else -> negatedParenthesizedExpressionText
            }
        }

        fun getNegation(element: JetExpression): JetExpression {
            return psiFactory.createExpression(when (element) {
                is JetBinaryExpression -> {
                    val operator = element.getOperationToken()!!

                    when {
                        isNegatableOperator(operator) -> "${element.getLeft()!!.getText()} ${getTokenText(getNegatedOperator(operator))} ${element.getRight()!!.getText()}"
                        else -> negateExpressionText(element)
                    }
                }
                is JetConstantExpression -> {
                    when {
                        element.textMatches("true") -> "false"
                        element.textMatches("false") -> "true"
                        else -> negateExpressionText(element)
                    }
                }
                else -> negateExpressionText(element)
            })
        }

        fun removeNegation(element: JetUnaryExpression): JetExpression {
            val baseExpression = element.getBaseExpression()!!

            return when (baseExpression) {
                is JetParenthesizedExpression -> baseExpression.getExpression()!!
                else -> baseExpression
            }
        }

        fun getFinalExpressionOfFunction(element: JetNamedFunction): JetExpression? {
            val body = element.getBodyExpression()

            return when (body) {
                is JetBlockExpression -> body.getStatements().last() as JetExpression
                else -> body
            }
        }

        val condition = element.getCondition()!!
        val replacementCondition = when (condition) {
            is JetUnaryExpression -> {
                when {
                    checkForNegation(condition) -> removeNegation(condition)
                    else -> getNegation(condition)
                }
            }
            else -> getNegation(condition)
        }

        val thenBranch = element.getThen()
        val elseBranch = element.getElse() ?: psiFactory.createEmptyBody()

        element.replace(psiFactory.createIf(replacementCondition, when (elseBranch) {
            is JetIfExpression -> psiFactory.wrapInABlock(elseBranch)
            else -> elseBranch
        }, if (thenBranch is JetBlockExpression && thenBranch.getStatements().isEmpty()) null else thenBranch))
    }
}