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
import org.jetbrains.kotlin.codegen.range.forLoop.ForInRangeLiteralLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForInUntilConstantRangeLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class PrimitiveNumberRangeLiteralRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun getBoundedValue(codegen: ExpressionCodegen) =
            SimpleBoundedValue(codegen, rangeCall)

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator =
            getConstRangeForInRangeLiteralGenerator(codegen, forExpression) ?:
            ForInRangeLiteralLoopGenerator(codegen, forExpression, rangeCall)

    private fun getConstRangeForInRangeLiteralGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator? {
        val rhsExpression = rangeCall.valueArgumentsByIndex?.run { get(0).arguments[0].getArgumentExpression() } ?: return null
        val constValue = codegen.getCompileTimeConstant(rhsExpression).safeAs<IntegerValueConstant<*>>() ?: return null
        val untilValue = when (constValue) {
            is ByteValue -> constValue.value + 1
            is ShortValue -> constValue.value + 1
            is IntValue -> constValue.value + 1
            else -> return null
        }
        // Watch out for integer overflow
        if (untilValue == Int.MIN_VALUE) return null
        return ForInUntilConstantRangeLoopGenerator(codegen, forExpression, rangeCall, untilValue)
    }
}