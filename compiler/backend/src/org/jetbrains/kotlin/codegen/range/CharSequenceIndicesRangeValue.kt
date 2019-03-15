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
import org.jetbrains.kotlin.codegen.range.comparison.IntComparisonGenerator
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForKotlinType
import org.jetbrains.kotlin.codegen.range.forLoop.ForInDefinitelySafeSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.ForInSimpleProgressionLoopGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

class CharSequenceIndicesRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>) :
    PrimitiveNumberRangeIntrinsicRangeValue(rangeCall), ReversableRangeValue {

    private val expectedReceiverType: KotlinType = ExpressionCodegen.getExpectedReceiverType(rangeCall)

    override fun getBoundedValue(codegen: ExpressionCodegen) =
        SimpleBoundedValue(
            codegen.asmType(rangeCall.resultingDescriptor.returnType!!),
            StackValue.constant(0, codegen.asmType(elementKotlinType), elementKotlinType),
            true,
            StackValue.operation(Type.INT_TYPE) { v ->
                codegen.generateCallReceiver(rangeCall).put(codegen.asmType(expectedReceiverType), expectedReceiverType, v)
                v.invokeinterface("java/lang/CharSequence", "length", "()I")
            },
            false
        )

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
        ForInSimpleProgressionLoopGenerator.fromBoundedValueWithStep1(
            codegen, forExpression, getBoundedValue(codegen), IntComparisonGenerator
        )

    override fun createForInReversedLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
        ForInDefinitelySafeSimpleProgressionLoopGenerator.fromBoundedValueWithStepMinus1(
            codegen, forExpression, getBoundedValue(codegen), IntComparisonGenerator
        )
}