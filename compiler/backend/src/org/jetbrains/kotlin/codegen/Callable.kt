/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface Callable {
    val owner: Type

    val dispatchReceiverType: Type?

    val dispatchReceiverKotlinType: KotlinType?

    val extensionReceiverType: Type?

    val extensionReceiverKotlinType: KotlinType?

    val generateCalleeType: Type?

    val valueParameterTypes: List<Type>

    val parameterTypes: Array<Type>

    val returnType: Type

    val returnKotlinType: KotlinType?

    fun genInvokeInstruction(v: InstructionAdapter)

    fun isStaticCall(): Boolean

    fun invokeMethodWithArguments(resolvedCall: ResolvedCall<*>, receiver: StackValue, codegen: ExpressionCodegen): StackValue {
        return StackValue.functionCall(returnType, resolvedCall.resultingDescriptor.returnType) {
            codegen.invokeMethodWithArguments(this, resolvedCall, receiver)
        }
    }

    fun afterReceiverGeneration(v: InstructionAdapter, frameMap: FrameMap) {
    }

}
