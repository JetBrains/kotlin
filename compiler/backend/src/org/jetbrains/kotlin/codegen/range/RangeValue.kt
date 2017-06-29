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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface RangeValue {
    fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator
}

class ArrayRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInArrayLoopGenerator(codegen, forExpression)
}

class PrimitiveRangeRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInRangeInstanceLoopGenerator(codegen, forExpression)
}

class PrimitiveProgressionRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInProgressionExpressionLoopGenerator(codegen, forExpression)
}

class CharSequenceRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCharSequenceLoopGenerator(codegen, forExpression)
}

class IterableRangeValue : RangeValue {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            IteratorForLoopGenerator(codegen, forExpression)
}

abstract class CallIntrinsicRangeValue(protected val loopRangeCall: ResolvedCall<out CallableDescriptor>): RangeValue

class PrimitiveNumberRangeToRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInRangeLiteralLoopGenerator(codegen, forExpression, loopRangeCall)
}

class DownToProgressionRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInDownToProgressionLoopGenerator(codegen, forExpression, loopRangeCall)
}

class PrimitiveNumberUntilRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInUntilRangeLoopGenerator(codegen, forExpression, loopRangeCall)
}

class ArrayIndicesRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInArrayIndicesRangeLoopGenerator(codegen, forExpression, loopRangeCall)
}

class CollectionIndicesRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCollectionIndicesRangeLoopGenerator(codegen, forExpression, loopRangeCall)
}

class CharSequenceIndicesRangeValue(loopRangeCall: ResolvedCall<out CallableDescriptor>): CallIntrinsicRangeValue(loopRangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInCharSequenceIndicesRangeLoopGenerator(codegen, forExpression, loopRangeCall)
}

