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
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

class InCollectionIndicesGenerator(
        codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        rangeCall: ResolvedCall<*>
) : AbstractInPrimitiveNumberRangeExpressionGenerator(codegen, operatorReference, rangeCall, isInclusiveHighBound = false) {
    private val receiverValue: ReceiverValue = rangeCall.extensionReceiver!!
    private val expectedReceiverType: KotlinType = ExpressionCodegen.getExpectedReceiverType(rangeCall)

    override fun genLowBound(): StackValue = StackValue.constant(0, asmElementType)

    override fun genHighBound(): StackValue =
            StackValue.operation(Type.INT_TYPE) { v ->
                codegen.generateReceiverValue(receiverValue, false).put(codegen.asmType(expectedReceiverType), v)
                v.invokeinterface("java/util/Collection", "size", "()I")
            }
}