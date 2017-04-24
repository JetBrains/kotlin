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

package org.jetbrains.kotlin.codegen.forLoop

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

abstract class ForInOptimizedIndicesLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        loopRangeCall: ResolvedCall<*>
) : AbstractForInRangeLoopGenerator(codegen, forExpression) {
    protected val receiverValue: ReceiverValue = loopRangeCall.extensionReceiver!!
    protected val expectedReceiverType: KotlinType = ExpressionCodegen.getExpectedReceiverType(loopRangeCall)

    override fun storeRangeStartAndEnd() {
        loopParameter().store(StackValue.constant(0, asmElementType), codegen.v)

        val receiver = codegen.generateReceiverValue(receiverValue, false)
        val receiverType = codegen.asmType(expectedReceiverType)
        receiver.put(receiverType, codegen.v)
        getReceiverSizeAsInt()
        codegen.v.iconst(1)
        codegen.v.sub(Type.INT_TYPE)
        StackValue.local(endVar, asmElementType).store(StackValue.onStack(Type.INT_TYPE), codegen.v)
    }

    /**
     * `(receiver -> size:I)`
     */
    protected abstract fun getReceiverSizeAsInt()
}
