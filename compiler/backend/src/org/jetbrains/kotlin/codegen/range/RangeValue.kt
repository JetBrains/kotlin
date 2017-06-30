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
import org.jetbrains.kotlin.codegen.forLoop.*
import org.jetbrains.kotlin.codegen.isClosedRangeContains
import org.jetbrains.kotlin.codegen.isPrimitiveRange
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface RangeValue {
    fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator

    fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator
}

class ArrayRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInArrayLoopGenerator(codegen, forExpression)

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
            CallBasedInExpressionGenerator(codegen, operatorReference)
}

class PrimitiveRangeRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInRangeInstanceLoopGenerator(codegen, forExpression)

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
            CallBasedInExpressionGenerator(codegen, operatorReference)
}

class PrimitiveProgressionRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInProgressionExpressionLoopGenerator(codegen, forExpression)

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
            CallBasedInExpressionGenerator(codegen, operatorReference)
}

class CharSequenceRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCharSequenceLoopGenerator(codegen, forExpression)

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
            CallBasedInExpressionGenerator(codegen, operatorReference)
}

class IterableRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            IteratorForLoopGenerator(codegen, forExpression)

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
            CallBasedInExpressionGenerator(codegen, operatorReference)
}

abstract class CallIntrinsicRangeValue(protected val rangeCall: ResolvedCall<out CallableDescriptor>): RangeValue {
    protected abstract fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>): Boolean

    protected abstract fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator {
        val resolvedCall = operatorReference.getResolvedCallWithAssert(codegen.bindingContext)
        return if (isIntrinsicInCall(resolvedCall))
            createIntrinsicInExpressionGenerator(codegen, operatorReference)
        else
            CallBasedInExpressionGenerator(codegen, operatorReference)
    }
}

abstract class PrimitiveNumberRangeIntrinsicRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(rangeCall) {
    override fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>) =
            resolvedCallForIn.resultingDescriptor.dispatchReceiverParameter?.let {
                isPrimitiveRange(it.type)
            } ?: false
}

class PrimitiveNumberRangeLiteralRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInRangeLiteralLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InPrimitiveNumberRangeLiteralGenerator(codegen, operatorReference, rangeCall)
}

class DownToProgressionRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInDownToProgressionLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InPrimitiveNumberDownToGenerator(codegen, operatorReference, rangeCall)
}

class PrimitiveNumberUntilRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInUntilRangeLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InPrimitiveNumberUntilGenerator(codegen, operatorReference, rangeCall)
}

class ArrayIndicesRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInArrayIndicesRangeLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InArrayIndicesGenerator(codegen, operatorReference, rangeCall)
}

class CollectionIndicesRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCollectionIndicesRangeLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InCollectionIndicesGenerator(codegen, operatorReference, rangeCall)
}

class CharSequenceIndicesRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCharSequenceIndicesRangeLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InCharSequenceIndicesGenerator(codegen, operatorReference, rangeCall)
}

class ComparableRangeLiteralRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            IteratorForLoopGenerator(codegen, forExpression)

    override fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>) =
            isClosedRangeContains(resolvedCallForIn.resultingDescriptor)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression)=
            InComparableRangeLiteralGenerator(codegen, operatorReference, rangeCall)
}