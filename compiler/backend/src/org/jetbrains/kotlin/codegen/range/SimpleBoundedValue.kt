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

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class SimpleBoundedValue(
        codegen: ExpressionCodegen,
        rangeCall: ResolvedCall<out CallableDescriptor>,
        private val lowBound: StackValue,
        isLowInclusive: Boolean,
        private val highBound: StackValue,
        isHighInclusive: Boolean
): AbstractBoundedValue(codegen, rangeCall, isLowInclusive, isHighInclusive) {
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

    override fun putHighLow(v: InstructionAdapter, type: Type) {
        if (!lowBound.canHaveSideEffects() || !highBound.canHaveSideEffects()) {
            highBound.put(type, v)
            lowBound.put(type, v)
        }
        else {
            lowBound.put(type, v)
            highBound.put(type, v)
            AsmUtil.swap(v, type, type)
        }
    }
}