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
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.generateCallReceiver
import org.jetbrains.kotlin.codegen.generateCallSingleArgument
import org.jetbrains.kotlin.codegen.range.forLoop.ForInDefinitelySafeSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForInSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.IntegerValueConstant
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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
        val endExpression = rangeCall.valueArgumentsByIndex?.run { get(0).arguments[0].getArgumentExpression() } ?: return null
        return createConstBoundedForLoopGenerator(
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
        val endExpression = rangeCall.extensionReceiver.safeAs<ExpressionReceiver>()?.expression ?: return null
        return createConstBoundedForLoopGenerator(
                codegen, forExpression,
                codegen.generateCallSingleArgument(rangeCall),
                endExpression,
                -1
        )
    }

    private fun createConstBoundedForLoopGenerator(
            codegen: ExpressionCodegen,
            forExpression: KtForExpression,
            startValue: StackValue,
            endExpression: KtExpression,
            step: Int
    ) : ForLoopGenerator? {
        val endConstValue = codegen.getCompileTimeConstant(endExpression).safeAs<IntegerValueConstant<*>>() ?: return null
        val endIntValue = when (endConstValue) {
            is ByteValue -> endConstValue.value.toInt()
            is ShortValue -> endConstValue.value.toInt()
            is IntValue -> endConstValue.value
            else -> return null
        }

        return if (isProhibitedIntConstEndValue(step, endIntValue))
            null
        else
            ForInDefinitelySafeSimpleProgressionLoopGenerator(
                    codegen, forExpression,
                    startValue = startValue,
                    isStartInclusive = true,
                    endValue = StackValue.integerConstant(endIntValue, asmElementType),
                    isEndInclusive = true,
                    step = step
            )
    }

    private fun isProhibitedIntConstEndValue(step: Int, endValue: Int) =
            endValue == if (step == 1) Int.MAX_VALUE else Int.MIN_VALUE

}