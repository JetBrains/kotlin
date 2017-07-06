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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class SimpleBoundedValue(
        private val codegen: ExpressionCodegen,
        private val rangeCall: ResolvedCall<out CallableDescriptor>,
        private val lowBound: StackValue,
        override val isLowInclusive: Boolean,
        private val highBound: StackValue,
        override val isHighInclusive: Boolean
): BoundedValue {
    constructor(
            codegen: ExpressionCodegen,
            rangeCall: ResolvedCall<out CallableDescriptor>,
            isLowInclusive: Boolean = true,
            isHighInclusive: Boolean = true
    ) : this(
            codegen,
            rangeCall,
            codegen.generateCallReceiver(rangeCall),
            isLowInclusive,
            codegen.generateCallSingleArgument(rangeCall),
            isHighInclusive
    )

    constructor(
            codegen: ExpressionCodegen,
            rangeCall: ResolvedCall<out CallableDescriptor>,
            lowBound: StackValue,
            highBound: StackValue
    ) : this(codegen, rangeCall, lowBound, true, highBound, true)

    override val instanceType: Type = codegen.asmType(rangeCall.resultingDescriptor.returnType!!)

    override fun putInstance(v: InstructionAdapter, type: Type) {
        codegen.invokeFunction(rangeCall.call, rangeCall, StackValue.none()).put(type, v)
    }

    override fun putHighLow(v: InstructionAdapter, type: Type) {
        highBound.put(type, v)
        lowBound.put(type, v)
    }

    companion object {

    }
}