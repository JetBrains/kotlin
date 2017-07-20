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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.isFlexible

class SimplifyBooleanWithConstantsInspection : IntentionBasedInspection<KtBinaryExpression>(SimplifyBooleanWithConstantsIntention::class)

class SimplifyBooleanWithConstantsIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Simplify boolean expression") {

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        val topBinary = PsiTreeUtil.getTopmostParentOfType(element, KtBinaryExpression::class.java) ?: element
        return areThereExpressionsToBeSimplified(topBinary)
    }

    private fun areThereExpressionsToBeSimplified(element: KtExpression?) : Boolean {
        if (element == null) return false
        when (element) {
            is KtParenthesizedExpression -> return areThereExpressionsToBeSimplified(element.expression)

            is KtBinaryExpression -> {
                val op = element.operationToken
                if (op == ANDAND || op == OROR || op == EQEQ || op == EXCLEQ) {
                    if (areThereExpressionsToBeSimplified(element.left) && element.right.hasBooleanType()) return true
                    if (areThereExpressionsToBeSimplified(element.right) && element.left.hasBooleanType()) return true
                }
            }
        }
        return element.canBeReducedToBooleanConstant()
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val topBinary = PsiTreeUtil.getTopmostParentOfType(element, KtBinaryExpression::class.java) ?: element
        val simplified = toSimplifiedExpression(topBinary)
        topBinary.replace(KtPsiUtil.safeDeparenthesize(simplified, true))
    }

    private fun toSimplifiedExpression(expression: KtExpression): KtExpression {
        val psiFactory = KtPsiFactory(expression)

        when  {
            expression.canBeReducedToTrue() -> {
                return psiFactory.createExpression("true")
            }

            expression.canBeReducedToFalse() -> {
                return psiFactory.createExpression("false")
            }

            expression is KtParenthesizedExpression -> {
                val expr = expression.expression
                if (expr != null) {
                    val simplified = toSimplifiedExpression(expr)
                    return if (simplified is KtBinaryExpression) {
                        // wrap in new parentheses to keep the user's original format
                        psiFactory.createExpressionByPattern("($0)", simplified)
                    }
                    else {
                        // if we now have a simpleName, constant, or parenthesized we don't need parentheses
                        simplified
                    }
                }
            }

            expression is KtBinaryExpression -> {
                val left = expression.left
                val right = expression.right
                val op = expression.operationToken
                if (left != null && right != null && (op == ANDAND || op == OROR || op == EQEQ || op == EXCLEQ)) {
                    val simpleLeft = simplifyExpression(left)
                    val simpleRight = simplifyExpression(right)
                    return when {
                        simpleLeft.canBeReducedToTrue() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(true, simpleRight, op)

                        simpleLeft.canBeReducedToFalse() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(false, simpleRight, op)

                        simpleRight.canBeReducedToTrue() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(true, simpleLeft, op)

                        simpleRight.canBeReducedToFalse() -> toSimplifiedBooleanBinaryExpressionWithConstantOperand(false, simpleLeft, op)

                        else -> {
                            val opText = expression.operationReference.text
                            psiFactory.createExpressionByPattern("$0 $opText $1", simpleLeft, simpleRight)
                        }
                    }
                }
            }
        }

        return expression.copied()
    }

    private fun toSimplifiedBooleanBinaryExpressionWithConstantOperand(constantOperand: Boolean, otherOperand: KtExpression, operation: IElementType): KtExpression {
        val factory = KtPsiFactory(otherOperand)
        when (operation) {
            OROR -> {
                if (constantOperand) return factory.createExpression("true")
            }
            ANDAND -> {
                if (!constantOperand) return factory.createExpression("false")
            }
            EQEQ, EXCLEQ -> toSimplifiedExpression(otherOperand).let {
                return if (constantOperand == (operation == EQEQ)) it
                else factory.createExpressionByPattern("!$0", it)
            }
        }
        return toSimplifiedExpression(otherOperand)
    }

    private fun simplifyExpression(expression: KtExpression) = expression.replaced(toSimplifiedExpression(expression))

    private fun KtExpression?.hasBooleanType(): Boolean {
        val type = this?.getType(this.analyze()) ?: return false
        return KotlinBuiltIns.isBoolean(type) && !type.isFlexible()
    }

    private fun KtExpression.canBeReducedToBooleanConstant(constant: Boolean? = null): Boolean {
        return CompileTimeConstantUtils.canBeReducedToBooleanConstant(this, this.analyze(), constant)
    }

    private fun KtExpression.canBeReducedToTrue() = canBeReducedToBooleanConstant(true)

    private fun KtExpression.canBeReducedToFalse() = canBeReducedToBooleanConstant(false)
}
