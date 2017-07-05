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
import org.jetbrains.kotlin.codegen.getAsmRangeElementTypeForPrimitiveRange
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForPrimitiveType
import org.jetbrains.kotlin.codegen.range.forLoop.ForInRangeLiteralLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InContinuousRangeExpressionGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class PrimitiveNumberRangeLiteralRangeValue(
        private val codegen: ExpressionCodegen,
        rangeCall: ResolvedCall<out CallableDescriptor>
): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall), BoundedValue {
    private val asmElementType = getAsmRangeElementTypeForPrimitiveRange(rangeCall.resultingDescriptor)
    private val from: ReceiverValue = rangeCall.dispatchReceiver ?: rangeCall.extensionReceiver!!
    private val to: KtExpression = ExpressionCodegen.getSingleArgumentExpression(rangeCall)!!

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInRangeLiteralLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InContinuousRangeExpressionGenerator(operatorReference, this, getComparisonGeneratorForPrimitiveType(asmElementType))

    override val instanceType: Type =
            codegen.asmType(rangeCall.resultingDescriptor.returnType!!)

    override fun putInstance(v: InstructionAdapter) {
        codegen.invokeFunction(rangeCall.call, rangeCall, StackValue.none()).put(instanceType, v)
    }

    override fun putHighLow(v: InstructionAdapter, type: Type) {
        codegen.gen(to).put(type, v)
        codegen.generateReceiverValue(from, false).put(type, v)
    }

    override val isLowInclusive: Boolean = true
    override val isHighInclusive: Boolean = true
}