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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.KotlinType

class ConvertTwoComparisonsToRangeCheckIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Convert to range check") {

    private data class RangeExpressionData(val value: String, val min: String, val max: String)

    override fun isApplicableTo(condition: KtBinaryExpression) = generateRangeExpressionData(condition) != null

    override fun applyTo(condition: KtBinaryExpression, editor: Editor?) {
        val rangeData = generateRangeExpressionData(condition) ?: return
        condition.replace(KtPsiFactory(condition).createExpressionByPattern("$0 in $1..$2", rangeData.value, rangeData.min, rangeData.max))
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

        return when (firstOpToken) {
            KtTokens.GTEQ, KtTokens.GT -> {
                when (secondOpToken) {
                    KtTokens.GTEQ, KtTokens.GT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstLeft, firstRight, secondLeft, incrementByOne = firstOpToken == KtTokens.GT, decrementByOne = secondOpToken == KtTokens.GT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstRight, secondRight, firstLeft, incrementByOne = firstOpToken == KtTokens.GT, decrementByOne = secondOpToken == KtTokens.GT)
                        else -> null
                    }
                    KtTokens.LTEQ, KtTokens.LT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstLeft, firstRight, secondRight, incrementByOne = firstOpToken == KtTokens.GT, decrementByOne = secondOpToken == KtTokens.LT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstRight, secondLeft, firstLeft, incrementByOne = firstOpToken == KtTokens.GT, decrementByOne = secondOpToken == KtTokens.LT)
                        else -> null
                    }
                    else -> null
                }
            }
            KtTokens.LTEQ, KtTokens.LT -> {
                when (secondOpToken) {
                    KtTokens.GTEQ, KtTokens.GT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstLeft, secondRight, firstRight, incrementByOne = firstOpToken == KtTokens.LT, decrementByOne = secondOpToken == KtTokens.GT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstRight, firstLeft, secondLeft, incrementByOne = firstOpToken == KtTokens.LT, decrementByOne = secondOpToken == KtTokens.GT)
                        else -> null
                    }
                    KtTokens.LTEQ, KtTokens.LT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstLeft, secondLeft, firstRight, incrementByOne = firstOpToken == KtTokens.LT, decrementByOne = secondOpToken == KtTokens.LT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstRight, firstLeft, secondRight, incrementByOne = firstOpToken == KtTokens.LT, decrementByOne = secondOpToken == KtTokens.LT)
                        else -> null
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun generateRangeExpressionData(value: KtExpression, min: KtExpression, max: KtExpression, incrementByOne: Boolean = false, decrementByOne: Boolean = false): RangeExpressionData? {
        fun KtExpression.getChangeBy(number: Int): String? {
            val type = getType(analyze()) ?: return null
            if (!type.isValidTypeForIncrementDecrementByOne()) return null

            when (this) {
                is KtConstantExpression -> {
                    return when {
                        KotlinBuiltIns.isInt(type) -> (text.toInt() + number).toString()
                        KotlinBuiltIns.isLong(type) -> {
                            val text = text
                            val longText = if (text.endsWith("l") || text.endsWith("L")) text.substring(0, text.length - 1) else text
                            (longText.toLong() + number).toString()
                        }
                        KotlinBuiltIns.isShort(type) -> (java.lang.Short.parseShort(text) + number).toString()
                        KotlinBuiltIns.isChar(type) -> "${text[0]}${text[1] + number}${text[2]}"
                        else -> return null
                    }
                }
                else -> return if (number > 0) "($text + $number)" else "($text - ${Math.abs(number)})"
            }
        }

        if (incrementByOne || decrementByOne) {
            if (!value.getType(value.analyze()).isValidTypeForIncrementDecrementByOne()) return null
        }

        val minText = if (incrementByOne) min.getChangeBy(1) else min.text
        val maxText = if (decrementByOne) max.getChangeBy(-1) else max.text
        return RangeExpressionData(value.text, minText ?: return null, maxText ?: return null)
    }

    fun KotlinType?.isValidTypeForIncrementDecrementByOne(): Boolean {
        this ?: return false
        return KotlinBuiltIns.isInt(this) || KotlinBuiltIns.isLong(this) || KotlinBuiltIns.isShort(this) || KotlinBuiltIns.isChar(this)
    }

}