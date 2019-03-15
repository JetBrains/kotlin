/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

interface ComparisonIntrinsic {
    fun genStackValue(expression: IrMemberAccessExpression, context: JvmBackendContext): StackValue
}

abstract class IntrinsicMethod {

    open fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        TODO()
    }

    open fun toCallable(method: CallableMethod): Callable {
        throw UnsupportedOperationException("Not implemented")
    }

    companion object {
        fun calcReceiverType(call: IrMemberAccessExpression, context: JvmBackendContext): Type {
            return context.state.typeMapper.mapType((call.dispatchReceiver?.type ?: call.extensionReceiver!!.type).toKotlinType())
        }

        fun expressionType(expression: IrExpression, context: JvmBackendContext): Type {
            return context.state.typeMapper.mapType(expression.type.toKotlinType())
        }

        fun JvmMethodSignature.newReturnType(type: Type): JvmMethodSignature {
            val newMethod = with(asmMethod) {
                Method(name, type, argumentTypes)
            }
            return JvmMethodSignature(newMethod, valueParameters)
        }
    }
}
