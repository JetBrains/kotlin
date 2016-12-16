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
                            generateRangeExpressionData(firstLeft, firstRight, secondLeft, minDecrementByOne = firstOpToken == KtTokens.GT, maxDecrementByOne = secondOpToken == KtTokens.GT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstRight, secondRight, firstLeft, minDecrementByOne = firstOpToken == KtTokens.GT, maxDecrementByOne = secondOpToken == KtTokens.GT)
                        else -> null
                    }
                    KtTokens.LTEQ, KtTokens.LT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstLeft, firstRight, secondRight, minDecrementByOne = firstOpToken == KtTokens.GT, maxDecrementByOne = secondOpToken == KtTokens.LT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstRight, secondLeft, firstLeft, minDecrementByOne = firstOpToken == KtTokens.GT, maxDecrementByOne = secondOpToken == KtTokens.LT)
                        else -> null
                    }
                    else -> null
                }
            }
            KtTokens.LTEQ, KtTokens.LT -> {
                when (secondOpToken) {
                    KtTokens.GTEQ, KtTokens.GT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstLeft, secondRight, firstRight, minDecrementByOne = firstOpToken == KtTokens.LT, maxDecrementByOne = secondOpToken == KtTokens.GT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstRight, firstLeft, secondLeft, minDecrementByOne = firstOpToken == KtTokens.LT, maxDecrementByOne = secondOpToken == KtTokens.GT)
                        else -> null
                    }
                    KtTokens.LTEQ, KtTokens.LT -> when {
                        firstLeft !is KtConstantExpression && firstLeft.evaluatesTo(secondRight) ->
                            generateRangeExpressionData(firstLeft, secondLeft, firstRight, minDecrementByOne = firstOpToken == KtTokens.LT, maxDecrementByOne = secondOpToken == KtTokens.LT)
                        firstRight !is KtConstantExpression && firstRight.evaluatesTo(secondLeft) ->
                            generateRangeExpressionData(firstRight, firstLeft, secondRight, minDecrementByOne = firstOpToken == KtTokens.LT, maxDecrementByOne = secondOpToken == KtTokens.LT)
                        else -> null
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun generateRangeExpressionData(value: KtExpression, min: KtExpression, max: KtExpression, minDecrementByOne: Boolean = false, maxDecrementByOne: Boolean = false): RangeExpressionData? {
        if (minDecrementByOne || maxDecrementByOne) {
            val type = value.getType(value.analyze()) ?: return null
            if (!KotlinBuiltIns.isInt(type) && !KotlinBuiltIns.isLong(type) && !KotlinBuiltIns.isShort(type) && !KotlinBuiltIns.isChar(type)) return null
        }

        val minText = if (minDecrementByOne) min.getDecrementByOneString() else min.text
        val maxText = if (maxDecrementByOne) max.getDecrementByOneString() else max.text
        return RangeExpressionData(value.text, minText ?: return null, maxText ?: return null)
    }

    private fun KtExpression.getDecrementByOneString(): String? {
        val type = getType(analyze()) ?: return null
        if (!KotlinBuiltIns.isInt(type) && !KotlinBuiltIns.isLong(type) && !KotlinBuiltIns.isShort(type) && !KotlinBuiltIns.isChar(type)) return null

        when (this) {
            is KtConstantExpression -> {
                val number: Number = when {
                    KotlinBuiltIns.isInt(type) -> text.toInt() - 1
                    KotlinBuiltIns.isLong(type) -> {
                        val text = text
                        val longText = if (text.endsWith("l") || text.endsWith("L")) text.substring(0, text.length - 1) else text
                        longText.toLong() - 1
                    }
                    KotlinBuiltIns.isShort(type) -> java.lang.Short.parseShort(text) - 1
                    KotlinBuiltIns.isChar(type) -> text[0].toInt() - 1
                    else -> return null
                }
                return number.toString()
            }
            else -> return "($text - 1)"
        }
    }
}