/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.KotlinType

class ConvertTwoComparisonsToRangeCheckInspection : IntentionBasedInspection<KtBinaryExpression>(
        ConvertTwoComparisonsToRangeCheckIntention::class
)

class ConvertTwoComparisonsToRangeCheckIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
        KtBinaryExpression::class.java,
        "Convert to range check"
) {

    private data class RangeExpressionData(val value: KtExpression, val min: String, val max: String)

    override fun isApplicableTo(element: KtBinaryExpression) = generateRangeExpressionData(element) != null

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val rangeData = generateRangeExpressionData(element) ?: return
        val factory = KtPsiFactory(element)
        element.replace(factory.createExpressionByPattern("$0 in $1..$2", rangeData.value,
                                                          factory.createExpression(rangeData.min),
                                                          factory.createExpression(rangeData.max)))
    }

    private fun generateRangeExpressionData(condition: KtBinaryExpression): RangeExpressionData? {
        if (condition.operationToken != KtTokens.ANDAND) return null
        val firstCondition = condition.left as? KtBinaryExpression ?: return null
        val secondCondition = condition.right as? KtBinaryExpression ?: return null
        val firstOpToken = firstCondition.operationToken
        val secondOpToken = secondCondition.operationToken
        val firstLeft = firstCondition.left ?: return null
        val firstRight = firstCondition.right ?: return null
        val secondLeft = secondCondition.left ?: return null
        val secondRight = secondCondition.right ?: return null

        fun IElementType.isStrictComparison() = this == KtTokens.GT || this == KtTokens.LT

        val firstStrict = firstOpToken.isStrictComparison()
        val secondStrict = secondOpToken.isStrictComparison()

        fun IElementType.orderLessAndGreater(left: KtExpression, right: KtExpression): Pair<KtExpression, KtExpression>? = when (this) {
            KtTokens.GTEQ, KtTokens.GT -> right to left
            KtTokens.LTEQ, KtTokens.LT -> left to right
            else -> null
        }

        val (firstLess, firstGreater) = firstOpToken.orderLessAndGreater(firstLeft, firstRight) ?: return null
        val (secondLess, secondGreater) = secondOpToken.orderLessAndGreater(secondLeft, secondRight) ?: return null

        return generateRangeExpressionData(firstLess, firstGreater, firstStrict, secondLess, secondGreater, secondStrict)
    }

    private fun KtExpression.isSimple() = this is KtConstantExpression || this is KtNameReferenceExpression

    private fun generateRangeExpressionData(
            firstLess: KtExpression, firstGreater: KtExpression, firstStrict: Boolean,
            secondLess: KtExpression, secondGreater: KtExpression, secondStrict: Boolean
    ) = when {
        firstGreater !is KtConstantExpression && firstGreater.evaluatesTo(secondLess) ->
            generateRangeExpressionData(firstGreater,
                                        min = firstLess,
                                        max = secondGreater,
                                        incrementMinByOne = firstStrict,
                                        decrementMaxByOne = secondStrict)
        firstLess !is KtConstantExpression && firstLess.evaluatesTo(secondGreater) ->
            generateRangeExpressionData(firstLess,
                                        min = secondLess,
                                        max = firstGreater,
                                        incrementMinByOne = secondStrict,
                                        decrementMaxByOne = firstStrict)
        else ->
            null
    }

    private fun generateRangeExpressionData(
            value: KtExpression, min: KtExpression, max: KtExpression, incrementMinByOne: Boolean, decrementMaxByOne: Boolean
    ): RangeExpressionData? {
        fun KtExpression.getChangeBy(number: Int): String? {
            val context = analyze()
            val type = getType(context) ?: return null
            if (!type.isValidTypeForIncrementDecrementByOne()) return null

            when (this) {
                is KtConstantExpression -> {
                    val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.getValue(type) ?: return null
                    return when {
                        KotlinBuiltIns.isInt(type) -> (constantValue as Int + number).toString()
                        KotlinBuiltIns.isLong(type) -> (constantValue as Long + number).toString()
                        KotlinBuiltIns.isChar(type) -> "'${constantValue as Char + number}'"
                        else -> return null
                    }
                }
                else -> return if (number >= 0) "($text + $number)" else "($text - ${-number})"
            }
        }

        // To avoid possible side effects
        if (!min.isSimple() || !max.isSimple()) return null

        if (incrementMinByOne || decrementMaxByOne) {
            if (!value.getType(value.analyze()).isValidTypeForIncrementDecrementByOne()) return null
        }

        val minText = if (incrementMinByOne) min.getChangeBy(1) else min.text
        val maxText = if (decrementMaxByOne) max.getChangeBy(-1) else max.text
        return RangeExpressionData(value, minText ?: return null, maxText ?: return null)
    }

    private fun KotlinType?.isValidTypeForIncrementDecrementByOne(): Boolean {
        this ?: return false
        return KotlinBuiltIns.isInt(this) ||
               KotlinBuiltIns.isLong(this) ||
               KotlinBuiltIns.isShort(this) ||
               KotlinBuiltIns.isByte(this) ||
               KotlinBuiltIns.isChar(this)
    }
}