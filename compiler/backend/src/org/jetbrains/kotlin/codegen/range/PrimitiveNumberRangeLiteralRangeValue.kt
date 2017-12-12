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
import org.jetbrains.kotlin.codegen.generateCallReceiver
import org.jetbrains.kotlin.codegen.generateCallSingleArgument
import org.jetbrains.kotlin.codegen.range.forLoop.ForInSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class PrimitiveNumberRangeLiteralRangeValue(
        rangeCall: ResolvedCall<out CallableDescriptor>
) : PrimitiveNumberRangeIntrinsicRangeValue(rangeCall),
        ReversableRangeValue {

    override fun getBoundedValue(codegen: ExpressionCodegen) =
            SimpleBoundedValue(codegen, rangeCall)

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator =
            createConstBoundedForInRangeLiteralGenerator(codegen, forExpression) ?:
            ForInSimpleProgressionLoopGenerator.fromBoundedValueWithStep1(codegen, forExpression, getBoundedValue(codegen))

    override fun createForInReversedLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator =
            createConstBoundedRangeForInReversedRangeLiteralGenerator(codegen, forExpression) ?:
            ForInSimpleProgressionLoopGenerator.fromBoundedValueWithStepMinus1(codegen, forExpression, getBoundedValue(codegen))

    private fun createConstBoundedForInRangeLiteralGenerator(
            codegen: ExpressionCodegen,
            forExpression: KtForExpression
    ): ForLoopGenerator? {
        val endExpression = rangeCall.getFirstArgumentExpression() ?: return null
        return createConstBoundedForLoopGeneratorOrNull(
                codegen, forExpression,
                codegen.generateCallReceiver(rangeCall),
                endExpression,
                1
        )
    }

    private fun createConstBoundedRangeForInReversedRangeLiteralGenerator(
            codegen: ExpressionCodegen,
            forExpression: KtForExpression
    ): ForLoopGenerator? {
        val endExpression = rangeCall.getReceiverExpression() ?: return null
        return createConstBoundedForLoopGeneratorOrNull(
                codegen, forExpression,
                codegen.generateCallSingleArgument(rangeCall),
                endExpression,
                -1
        )
    }
}