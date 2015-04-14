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

import org.jetbrains.kotlin.psi.JetPrefixExpression
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetParenthesizedExpression
import org.jetbrains.kotlin.lexer.JetToken
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.psi.JetOperationExpression
import org.jetbrains.kotlin.psi.JetIsExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.JetPsiFactory

public class SimplifyNegatedBinaryExpressionIntention : JetSelfTargetingOffsetIndependentIntention<JetPrefixExpression>("simplify.negated.binary.expression", javaClass()) {

    private fun JetPrefixExpression.unparenthesize(): JetExpression? {
        return (this.getBaseExpression() as? JetParenthesizedExpression)?.getExpression()
    }

    public fun JetToken.negate(): JetSingleValueToken? = when (this) {
        JetTokens.IN_KEYWORD    -> JetTokens.NOT_IN
        JetTokens.NOT_IN        -> JetTokens.IN_KEYWORD

        JetTokens.IS_KEYWORD    -> JetTokens.NOT_IS
        JetTokens.NOT_IS        -> JetTokens.IS_KEYWORD

        JetTokens.EQEQ          -> JetTokens.EXCLEQ
        JetTokens.EXCLEQ        -> JetTokens.EQEQ

        JetTokens.LT            -> JetTokens.GTEQ
        JetTokens.GTEQ          -> JetTokens.LT

        JetTokens.GT            -> JetTokens.LTEQ
        JetTokens.LTEQ          -> JetTokens.GT

        else -> null
    }

    override fun isApplicableTo(element: JetPrefixExpression): Boolean {
        if (element.getOperationReference().getReferencedNameElementType() != JetTokens.EXCL) return false

        val expression = element.unparenthesize() as? JetOperationExpression
        if (!(expression is JetIsExpression || expression is JetBinaryExpression)) return false

        val operation = (JetPsiUtil.getOperationToken(expression) as? JetSingleValueToken) ?: return false
        val negOperation = operation.negate() ?: return false

        setText(JetBundle.message("simplify.negated.binary.expression", operation.getValue(), negOperation.getValue()))
        return true
    }

    override fun applyTo(element: JetPrefixExpression, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: JetPrefixExpression) {
        // Guaranteed to succeed (by isApplicableTo)
        val expression = element.unparenthesize()!!
        val invertedOperation = JetPsiUtil.getOperationToken(expression as JetOperationExpression)!!.negate()!!

        val psiFactory = JetPsiFactory(expression)
        element.replace(
                when (expression) {
                    is JetIsExpression -> {
                        psiFactory.createExpression(
                                "${expression.getLeftHandSide().getText() ?: ""} ${invertedOperation.getValue()} ${expression.getTypeReference()?.getText() ?: ""}"
                        )
                    }
                    is JetBinaryExpression -> psiFactory.createBinaryExpression(
                            expression.getLeft(),
                            invertedOperation.getValue(),
                            expression.getRight()
                    )
                    else -> throw IllegalStateException(
                            "Expression is neither a JetIsExpression or JetBinaryExpression (checked by isApplicableTo): ${expression.getText()}"
                    )
                }
        )
    }
}
