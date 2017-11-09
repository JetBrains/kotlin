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
import org.jetbrains.kotlin.codegen.range.comparison.getComparisonGeneratorForRangeContainsCall
import org.jetbrains.kotlin.codegen.range.inExpression.CallBasedInExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

abstract class CallIntrinsicRangeValue(protected val rangeCall: ResolvedCall<out CallableDescriptor>): RangeValue {
    protected abstract fun isIntrinsicInCall(resolvedCallForIn: ResolvedCall<out CallableDescriptor>): Boolean

    protected abstract fun createIntrinsicInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression, resolvedCall: ResolvedCall<out CallableDescriptor>): InExpressionGenerator

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator {
        val resolvedCall = operatorReference.getResolvedCallWithAssert(codegen.bindingContext)
        return if (isIntrinsicInCall(resolvedCall))
            createIntrinsicInExpressionGenerator(codegen, operatorReference, resolvedCall)
        else
            CallBasedInExpressionGenerator(codegen, operatorReference)
    }
}