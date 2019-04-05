/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

abstract class IntrinsicMethod {
    open fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction = TODO("implement toCallable() or invoke() of $this")

    open fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? =
        with(codegen) {
            val descriptor = typeMapper.mapSignatureSkipGeneric(expression.descriptor)
            val stackValue = toCallable(expression, descriptor, context).invoke(mv, codegen, data)
            return object : PromisedValue(mv, stackValue.type) {
                override fun materialize() = stackValue.put(mv)
            }
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
