/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.lang.UnsupportedOperationException

open class IntrinsicCallable(
        override val returnType: Type,
        override val valueParameterTypes: List<Type>,
        override val dispatchReceiverType: Type?,
        override val extensionReceiverType: Type?,
        private val invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit = { throw UnsupportedOperationException() }
) : Callable {

    constructor(
            callable: CallableMethod,
            invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit = {}
    ) : this(
            callable.returnType,
            callable.valueParameterTypes,
            callable.dispatchReceiverType,
            callable.extensionReceiverType,
            invoke
    )

    override fun genInvokeInstruction(v: InstructionAdapter) {
        invokeIntrinsic(v)
    }

    open fun invokeIntrinsic(v: InstructionAdapter) {
        invoke(v)
    }

    override val parameterTypes: Array<Type>
        get() = throw UnsupportedOperationException()

    override fun isStaticCall() = false

    override val generateCalleeType: Type?
        get() = null

    override val owner: Type
        get() = throw UnsupportedOperationException()

    fun calcReceiverType(): Type =
            extensionReceiverType ?: dispatchReceiverType!!
}

fun createBinaryIntrinsicCallable(
        returnType: Type,
        valueParameterType: Type,
        thisType: Type? = null,
        receiverType: Type? = null,
        lambda: IntrinsicCallable.(v: InstructionAdapter) -> Unit
): IntrinsicCallable {
    assert(AsmUtil.isPrimitive(returnType)) { "Return type of BinaryOp intrinsic should be of primitive type: $returnType" }

    return object : IntrinsicCallable(returnType, listOf(valueParameterType), thisType, receiverType) {
        override fun invokeIntrinsic(v: InstructionAdapter) {
            lambda(v)
        }
    }
}

fun createUnaryIntrinsicCallable(
        callable: CallableMethod,
        newReturnType: Type? = null,
        needPrimitiveCheck: Boolean = false,
        newThisType: Type? = null,
        invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit
): IntrinsicCallable {
    val intrinsic = IntrinsicCallable(
            newReturnType ?: callable.returnType,
            callable.valueParameterTypes,
            newThisType ?: callable.dispatchReceiverType,
            callable.extensionReceiverType,
            invoke
    )
    assert(intrinsic.valueParameterTypes.isEmpty()) { "Unary operation should not have any parameters" }
    if (needPrimitiveCheck) {
        assert(AsmUtil.isPrimitive(intrinsic.returnType)) {
            "Return type of UnaryPlus intrinsic should be of primitive type: ${intrinsic.returnType}"
        }
    }
    return intrinsic
}

fun createIntrinsicCallable(
        callable: CallableMethod,
        invoke: IntrinsicCallable.(v: InstructionAdapter) -> Unit
): IntrinsicCallable {
    return IntrinsicCallable(callable, invoke)
}
