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
import org.jetbrains.kotlin.codegen.range.forLoop.ForInUntilRangeLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InPrimitiveNumberUntilGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

class PrimitiveNumberUntilRangeValue(rangeCall: ResolvedCall<out CallableDescriptor>): PrimitiveNumberRangeIntrinsicRangeValue(rangeCall) {
    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) =
            ForInUntilRangeLoopGenerator(codegen, forExpression, rangeCall)

    override fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression) =
            InPrimitiveNumberUntilGenerator(codegen, operatorReference, rangeCall)
}