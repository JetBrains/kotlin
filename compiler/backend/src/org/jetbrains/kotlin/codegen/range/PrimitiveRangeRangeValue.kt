/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForKotlinType
import org.jetbrains.kotlin.codegen.range.forLoop.ForInRangeInstanceLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.CallBasedInExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.types.KotlinType

class PrimitiveRangeRangeValue(private val rangeExpression: KtExpression) : ReversableRangeValue {

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
        ForInRangeInstanceLoopGenerator(
            codegen, forExpression, rangeExpression,
            getComparisonGeneratorForKotlinType(getRangeElementType(codegen, forExpression)),
            reversed = false
        )

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
        CallBasedInExpressionGenerator(codegen, operatorReference)

    override fun createForInReversedLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator =
        ForInRangeInstanceLoopGenerator(
            codegen, forExpression, rangeExpression,
            getComparisonGeneratorForKotlinType(getRangeElementType(codegen, forExpression)),
            reversed = true
        )

    private fun getRangeElementType(codegen: ExpressionCodegen, forExpression: KtForExpression): KotlinType {
        val ktLoopRange = forExpression.loopRange
            ?: throw AssertionError("No loop range expression: ${forExpression.text}")
        val rangeType = codegen.bindingContext.getType(ktLoopRange)
            ?: throw AssertionError("No type for loop range expression: ${ktLoopRange.text}")
        return getRangeOrProgressionElementType(rangeType)
            ?: throw AssertionError("Unexpected range type: $rangeType")
    }
}