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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class AbstractBoundedValue(
        protected val codegen: ExpressionCodegen,
        protected val rangeCall: ResolvedCall<out CallableDescriptor>,
        override val isLowInclusive: Boolean = true,
        override val isHighInclusive: Boolean = true
) : BoundedValue {
    override val instanceType: Type = codegen.asmType(rangeCall.resultingDescriptor.returnType!!)

    override fun putInstance(v: InstructionAdapter, type: Type) {
        codegen.invokeFunction(rangeCall.call, rangeCall, StackValue.none()).put(type, v)
    }
}