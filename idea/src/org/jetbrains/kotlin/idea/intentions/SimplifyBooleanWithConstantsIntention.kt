/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.isTrueConstant
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompileTimeConstantUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode.PARTIAL
import org.jetbrains.kotlin.types.isFlexible

@Suppress("DEPRECATION")
class SimplifyBooleanWithConstantsInspection : IntentionBasedInspection<KtBinaryExpression>(SimplifyBooleanWithConstantsIntention::class)

class SimplifyBooleanWithConstantsIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    KotlinBundle.lazyMessage("simplify.boolean.expression")
) {
    override fun isApplicableTo(element: KtBinaryExpression): Boolean = areThereExpressionsToBeSimplified(element.topBinary())

    private fun KtBinaryExpression.topBinary(): KtBinaryExpression =
        this.parentsWithSelf.takeWhile { it is KtBinaryExpression }.lastOrNull() as? KtBinaryExpression ?: this

    private fun areThereExpressionsToBeSimplified(element: KtExpression?): Boolean {
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
        val topBinary = element.topBinary()
        val simplified = toSimplifiedExpression(topBinary)
        val result = topBinary.replaced(KtPsiUtil.safeDeparenthesize(simplified, true))
        removeRedundantAssertion(result)
    }

    internal fun removeRedundantAssertion(expression: KtExpression) {
        val callExpression = expression.getNonStrictParentOfType<KtCallExpression>() ?: return
        val fqName = callExpression.getCallableDescriptor()?.fqNameOrNull()
        val valueArguments = callExpression.valueArguments
        val isRedundant = fqName?.asString() == "kotlin.assert" &&
                valueArguments.singleOrNull()?.getArgumentExpression().isTrueConstant()
        if (isRedundant) callExpression.delete()
    }

    private fun toSimplifiedExpression(expression: KtExpression): KtExpression {
        val psiFactory = KtPsiFactory(expression)

        when {
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
                    } else {
                        // if we now have a simpleName, constant, or parenthesized we don't need parentheses
                        simplified
                    }
                }
            }

            expression is KtBinaryExpression -> {
                if (!areThereExpressionsToBeSimplified(expression)) return expression.copied()
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

    private fun toSimplifiedBooleanBinaryExpressionWithConstantOperand(
        constantOperand: Boolean,
        otherOperand: KtExpression,
        operation: IElementType
    ): KtExpression {
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
        val type = this?.getType(this.analyze(PARTIAL)) ?: return false
        return KotlinBuiltIns.isBoolean(type) && !type.isFlexible()
    }

    private fun KtExpression.canBeReducedToBooleanConstant(constant: Boolean? = null): Boolean {
        val context = this.analyze(PARTIAL)
        if (this is KtBinaryExpression) {
            val operation = (this.operationToken as? KtSingleValueToken)?.value
            val left = this.left
            val right = this.right
            if (operation != null && left != null && right != null) {
                val psiFactory = KtPsiFactory(this)
                val leftConstantValue = left.constantValue(context)
                val rightConstantValue = right.constantValue(context)
                if (leftConstantValue.isFpMinusZero() || rightConstantValue.isFpMinusZero()) {
                    val newLeft = when (leftConstantValue) {
                        -0.0 -> psiFactory.createExpression("0.0")
                        -0.0f -> psiFactory.createExpression("0.0f")
                        else -> left
                    }
                    val newRight = when (rightConstantValue) {
                        -0.0 -> psiFactory.createExpression("0.0")
                        -0.0f -> psiFactory.createExpression("0.0f")
                        else -> right
                    }
                    val newExpression = psiFactory.createExpressionByPattern("$0 $1 $2", newLeft, operation, newRight, reformat = false)
                    val newContext = newExpression.analyzeAsReplacement(this, context)
                    return CompileTimeConstantUtils.canBeReducedToBooleanConstant(newExpression, newContext, constant)
                }
            }
        }
        return CompileTimeConstantUtils.canBeReducedToBooleanConstant(this, context, constant)
    }

    private fun KtExpression.canBeReducedToTrue() = canBeReducedToBooleanConstant(true)

    private fun KtExpression.canBeReducedToFalse() = canBeReducedToBooleanConstant(false)

    private fun KtExpression?.constantValue(context: BindingContext): Any? {
        val expression = KtPsiUtil.deparenthesize(this) ?: return null
        return (ConstantExpressionEvaluator.getConstant(expression, context) as? TypedCompileTimeConstant)?.constantValue?.value
    }

    private fun Any?.isFpMinusZero(): Boolean = this == -0.0 || this == -0.0f
}
