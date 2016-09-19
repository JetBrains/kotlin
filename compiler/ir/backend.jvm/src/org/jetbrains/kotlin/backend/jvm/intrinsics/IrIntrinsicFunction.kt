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

import org.jetbrains.kotlin.backend.jvm.BlockInfo
import org.jetbrains.kotlin.backend.jvm.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter


open class IrIntrinsicFunction(
        val expression: IrMemberAccessExpression,
        val signature: JvmMethodSignature,
        val argsTypes: List<Type> = signature.valueParameters.map { it.asmType }
) : Callable {
    override val owner: Type
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val dispatchReceiverType: Type?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val extensionReceiverType: Type?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val generateCalleeType: Type?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val valueParameterTypes: List<Type>
        get() = signature.valueParameters.map { it.asmType }
    override val parameterTypes: Array<Type>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val returnType: Type
        get() = signature.returnType

    override fun isStaticCall(): Boolean {
        return false
    }

    override fun genInvokeInstruction(v: InstructionAdapter) {
        TODO("not implemented for $this")
    }

    open fun invoke(v: InstructionAdapter, codegen: ExpressionCodegen, data: BlockInfo):StackValue {
        val args = mutableListOf(expression.dispatchReceiver, expression.extensionReceiver)
        args.addAll(expression.descriptor.valueParameters.mapIndexed {
            i, valueParameterDescriptor ->
            expression.getValueArgument(i)
        })
        args.filterNotNull().forEachIndexed { i, irExpression -> genArg(irExpression, codegen, i, data) }
        genInvokeInstruction(v)
        return StackValue.onStack(returnType)
    }

    fun genArg(expression: IrExpression, codegen: ExpressionCodegen, index: Int, data: BlockInfo) {
        codegen.gen(expression, argsTypes[index], data)
    }

    companion object {
        fun create(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   argsTypes: List<Type> = signature.valueParameters.map { it.asmType },
                   invokeInstuction: (InstructionAdapter) -> Unit): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, argsTypes) {
                override fun genInvokeInstruction(v: InstructionAdapter) {
                    invokeInstuction(v)
                }
            }
        }

        fun create(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   context: JvmBackendContext,
                   invokeInstuction: (InstructionAdapter) -> Unit): IrIntrinsicFunction {
            val args = (expression.dispatchReceiver?.let { listOf(context.state.typeMapper.mapType(it.type)) } ?: emptyList<Type>()) +
                       signature.valueParameters.map { it.asmType }
            return object : IrIntrinsicFunction(expression, signature, args) {
                override fun genInvokeInstruction(v: InstructionAdapter) {
                    invokeInstuction(v)
                }
            }
        }

        fun create(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   type: Type,
                   invokeInstuction: (InstructionAdapter) -> Unit): IrIntrinsicFunction {
            return create(expression, signature, listOf(type), invokeInstuction)
        }
    }
}