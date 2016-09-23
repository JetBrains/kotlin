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

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*


open class IrIntrinsicFunction(
        val expression: IrMemberAccessExpression,
        val signature: JvmMethodSignature,
        val context: JvmBackendContext,
        val argsTypes: List<Type> = expression.argTypes(context)
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

    open fun genArg(expression: IrExpression, codegen: ExpressionCodegen, index: Int, data: BlockInfo) {
        codegen.gen(expression, argsTypes[index], data)
    }

    companion object {
        fun create(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   context: JvmBackendContext,
                   argsTypes: List<Type> = expression.argTypes(context),
                   invokeInstuction: IrIntrinsicFunction.(InstructionAdapter) -> Unit): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {
                override fun genInvokeInstruction(v: InstructionAdapter) {
                    invokeInstuction(v)
                }
            }
        }

        fun create(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   context: JvmBackendContext,
                   type: Type,
                   invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Unit): IrIntrinsicFunction {
            return create(expression, signature, context, listOf(type), invokeInstruction)
        }
    }
}

fun IrMemberAccessExpression.argTypes(context: JvmBackendContext): ArrayList<Type> {
    val callableMethod = context.state.typeMapper.mapToCallableMethod(descriptor as FunctionDescriptor, false)
    val args = arrayListOf<Type>().apply {
        callableMethod.dispatchReceiverType?.let { add(it) }
        addAll(callableMethod.getAsmMethod().argumentTypes)
    }
    return args
}

fun IrMemberAccessExpression.receiverAndArgs(): List<IrExpression> {
    return (arrayListOf(this.dispatchReceiver, this.extensionReceiver) +
                 descriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->getValueArgument(i)}).filterNotNull()
}

fun List<IrExpression>.asmTypes(context: JvmBackendContext): List<Type> {
    return map { context.state.typeMapper.mapType(it.type) }
}