/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface Callable {
    val owner: Type

    val dispatchReceiverType: Type?

    val extensionReceiverType: Type?

    val generateCalleeType: Type?

    val valueParameterTypes: List<Type>

    val parameterTypes: Array<Type>

    val returnType: Type

    fun genInvokeInstruction(v: InstructionAdapter)

    fun isStaticCall(): Boolean

    fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, codegen: ExpressionCodegen): StackValue {
        return StackValue.functionCall(returnType) {
            codegen.invokeMethodWithArguments(this, resolvedCall, receiver)
        }
    }

    fun afterReceiverGeneration(v: InstructionAdapter) {
    }

}
