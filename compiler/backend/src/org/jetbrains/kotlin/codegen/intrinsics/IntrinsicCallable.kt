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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public abstract class IntrinsicCallable(override val returnType: Type,
                                        override val valueParameterTypes: List<Type>,
                                        override val thisType: Type?,
                                        override val receiverType: Type?) : Callable {

    override fun invokeWithoutAssertions(v: InstructionAdapter) {
        invokeIntrinsic(v)
    }

    override fun invokeWithNotNullAssertion(v: InstructionAdapter, state: GenerationState, resolvedCall: ResolvedCall<out CallableDescriptor>) {
        invokeWithoutAssertions(v)
    }

    public abstract fun invokeIntrinsic(v: InstructionAdapter);

    override val argumentTypes: Array<Type>
        get() {
            throw UnsupportedOperationException()
        }

    override fun isStaticCall(): Boolean {
        return false
    }

    override val generateCalleeType: Type?
        get() {
            return null
        }

    override val owner: Type
        get() {
            throw UnsupportedOperationException()
        }

    public fun calcReceiverType(): Type? {
        return receiverType ?: thisType
    }
}

fun createBinaryIntrinsicCallable(returnType: Type, valueParameterType: Type, thisType: Type? = null, receiverType: Type? = null, lambda: IntrinsicCallable.(i: InstructionAdapter) -> Unit): IntrinsicCallable {
    assert(AsmUtil.isPrimitive(returnType)) { "Return type of BinaryOp intrinsic should be of primitive type : " + returnType }

    return object : IntrinsicCallable(returnType, listOf(valueParameterType), thisType, receiverType) {
        override fun invokeIntrinsic(v: InstructionAdapter) {
            lambda(v)
        }
    }
}

public class UnaryIntrinsic(
        callable: CallableMethod,
        newReturnType: Type? = null,
        needPrimitiveCheck: Boolean = true,
        newThisType: Type? = null,
        private val invoke: UnaryIntrinsic.(v: InstructionAdapter) -> Unit
) : IntrinsicCallable(newReturnType ?: callable.returnType, callable.valueParameterTypes, newThisType ?: callable.thisType, callable.receiverType) {

    init {
        if (needPrimitiveCheck) {
            assert(AsmUtil.isPrimitive(returnType)) { "Return type of UnaryPlus intrinsic should be of primitive type: $returnType" }
        }
        assert(valueParameterTypes.isEmpty()) { "Unary operation should not have any parameters" }
    }

    override fun invokeIntrinsic(v: InstructionAdapter) {
        invoke(v)
    }
}

public fun createUnaryIntrinsicCallable(
        callable: CallableMethod,
        newReturnType: Type? = null,
        needPrimitiveCheck: Boolean = true,
        newThisType: Type? = null,
        invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit
) : IntrinsicCallable {
    return UnaryIntrinsic(callable, newReturnType, needPrimitiveCheck, newThisType, invoke = invoke)
}

public open class MappedCallable(val callable: CallableMethod, val invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit = {}) :
        IntrinsicCallable(callable.returnType, callable.valueParameterTypes, callable.thisType, callable.receiverType) {

    override fun invokeIntrinsic(v: InstructionAdapter) {
        invoke(v)
    }
}

public fun createIntrinsicCallable(callable: CallableMethod, invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit) : IntrinsicCallable {
    return MappedCallable(callable, invoke)
}