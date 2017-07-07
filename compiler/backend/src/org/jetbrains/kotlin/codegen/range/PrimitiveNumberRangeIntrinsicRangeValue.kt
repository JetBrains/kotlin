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

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForRangeContainsCall
import org.jetbrains.kotlin.codegen.range.inExpression.CallBasedInExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InPrimitiveContinuousRangeExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

abstract class PrimitiveNumberRangeIntrinsicRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(rangeCall) {
    protected val asmElementType = getAsmRangeElementTypeForPrimitiveRangeOrProgression(rangeCall.resultingDescriptor)

    override fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>) =
            resolvedCallForIn.resultingDescriptor.let {
                isPrimitiveRangeContains(it) ||
                isClosedFloatingPointRangeContains(it) ||
                isPrimitiveNumberRangeExtensionContainsPrimitiveNumber(it)
            }

    override fun createIntrinsicInExpressionGenerator(
            codegen: ExpressionCodegen,
            operatorReference: KtSimpleNameExpression,
            resolvedCall: ResolvedCall<out CallableDescriptor>
    ): InExpressionGenerator {
        val comparisonGenerator = getComparisonGeneratorForRangeContainsCall(codegen, resolvedCall)
        return if (comparisonGenerator != null)
            InPrimitiveContinuousRangeExpressionGenerator(operatorReference, getBoundedValue(codegen), comparisonGenerator)
        else
            CallBasedInExpressionGenerator(codegen, operatorReference)
    }

    protected abstract fun getBoundedValue(codegen: ExpressionCodegen): BoundedValue
}