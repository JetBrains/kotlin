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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.copied
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*

public class SimplifyBooleanWithConstantsIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Simplify boolean expression") {

    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        val topBinary = PsiTreeUtil.getTopmostParentOfType(element, javaClass<JetBinaryExpression>()) ?: element
        return areThereExpressionsToBeSimplified(topBinary)
    }

    private fun areThereExpressionsToBeSimplified(element: JetExpression?) : Boolean {
        if (element == null) return false
        when (element) {
            is JetParenthesizedExpression -> return areThereExpressionsToBeSimplified(element.getExpression())

            is JetBinaryExpression -> {
                val op = element.getOperationToken()
                if ((op == JetTokens.ANDAND || op == JetTokens.OROR) &&
                       (areThereExpressionsToBeSimplified(element.getLeft()) ||
                       areThereExpressionsToBeSimplified(element.getRight()))) return true
            }
        }
        return element.canBeReducedToBooleanConstant(null)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val topBinary = PsiTreeUtil.getTopmostParentOfType(element, javaClass<JetBinaryExpression>()) ?: element
        val simplified = toSimplifiedExpression(topBinary)
        topBinary.replace(JetPsiUtil.safeDeparenthesize(simplified))
    }

    private fun toSimplifiedExpression(expression: JetExpression): JetExpression {
        val psiFactory = JetPsiFactory(expression)

        when  {
            expression.canBeReducedToTrue() -> {
                return psiFactory.createExpression("true")
            }

            expression.canBeReducedToFalse() -> {
                return psiFactory.createExpression("false")
            }

            expression is JetParenthesizedExpression -> {
                val expr = expression.getExpression()
                if (expr != null) {
                    val simplified = toSimplifiedExpression(expr)
                    return if (simplified is JetBinaryExpression) {
                        // wrap in new parentheses to keep the user's original format
                        psiFactory.createExpressionByPattern("($0)", simplified)
                    }
                    else {
                        // if we now have a simpleName, constant, or parenthesized we don't need parentheses
                        simplified
                    }
                }
            }

            expression is JetBinaryExpression -> {
                val left = expression.getLeft()
                val right = expression.getRight()
                val op = expression.getOperationToken()
                if (left != null && right != null && (op == JetTokens.ANDAND || op == JetTokens.OROR)) {
                    val simpleLeft = simplifyExpression(left)
                    val simpleRight = simplifyExpression(right)
                    return when {
                        simpleLeft.canBeReducedToTrue() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(true, simpleRight, op)

                        simpleLeft.canBeReducedToFalse() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(false, simpleRight, op)

                        simpleRight.canBeReducedToTrue() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(true, simpleLeft, op)

                        simpleRight.canBeReducedToFalse() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(false, simpleLeft, op)

                        else -> {
                            val opText = expression.getOperationReference().getText()
                            psiFactory.createExpressionByPattern("$0 $opText $1", simpleLeft, simpleRight)
                        }
                    }
                }
            }
        }

        return expression.copied()
    }

    private fun toSimplifiedBooleanBinaryExpressionWithConstantOperand(constantOperand: Boolean, otherOperand: JetExpression, operation: IElementType): JetExpression {
        return when {
            constantOperand && operation == JetTokens.OROR -> JetPsiFactory(otherOperand).createExpression("true")
            !constantOperand && operation == JetTokens.ANDAND -> JetPsiFactory(otherOperand).createExpression("false")
            else -> toSimplifiedExpression(otherOperand)
        }
    }

    private fun simplifyExpression(expression: JetExpression) = expression.replaced(toSimplifiedExpression(expression))

    private fun JetExpression.canBeReducedToBooleanConstant(constant: Boolean?): Boolean {
        val bindingContext = this.analyze()
        val trace = DelegatingBindingTrace(bindingContext, "trace for constant check")
        return CompileTimeConstantUtils.canBeReducedToBooleanConstant(this, trace, constant)
    }

    private fun JetExpression.canBeReducedToTrue() = canBeReducedToBooleanConstant(true)

    private fun JetExpression.canBeReducedToFalse() = canBeReducedToBooleanConstant(false)
}
