/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

private class IrEmptyVarargExpression(
    override val type: IrType,
    override val startOffset: Int,
    override val endOffset: Int
) : IrExpression {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        TODO("not implemented")
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        TODO("not implemented")
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        TODO("not implemented")
    }
}

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
    override val dispatchReceiverKotlinType: KotlinType?
        get() = null
    override val extensionReceiverType: Type?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val extensionReceiverKotlinType: KotlinType?
        get() = null
    override val generateCalleeType: Type?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val valueParameterTypes: List<Type>
        get() = signature.valueParameters.map { it.asmType }
    override val parameterTypes: Array<Type>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val returnType: Type
        get() = signature.returnType
    override val returnKotlinType: KotlinType?
        get() = null

    override fun isStaticCall(): Boolean {
        return false
    }

    override fun genInvokeInstruction(v: InstructionAdapter) {
        TODO("not implemented for $this")
    }

    open fun genInvokeInstructionWithResult(v: InstructionAdapter): Type {
        genInvokeInstruction(v)
        return returnType
    }

    open fun invoke(v: InstructionAdapter, codegen: ExpressionCodegen, data: BlockInfo): StackValue {
        loadArguments(codegen, data)
        return StackValue.onStack(genInvokeInstructionWithResult(v))
    }

    fun loadArguments(
        codegen: ExpressionCodegen,
        data: BlockInfo
    ) {
        val args = listOfNotNull(expression.dispatchReceiver, expression.extensionReceiver) +
                expression.descriptor.valueParameters.mapIndexed { i, descriptor ->
                    expression.getValueArgument(i) ?: if (descriptor.isVararg)
                        IrEmptyVarargExpression(descriptor.type.toIrType()!!, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
                    else error("Unknown parameter: $descriptor in $expression")
                }

        args.forEachIndexed { i, irExpression ->
            if (irExpression is IrEmptyVarargExpression) {
                val parameterType = codegen.typeMapper.mapType(irExpression.type.toKotlinType())
                StackValue.operation(parameterType) {
                    it.aconst(0)
                    it.newarray(AsmUtil.correctElementType(parameterType))
                }.put(parameterType, codegen.mv)
            } else {
                genArg(irExpression, codegen, i, data)
            }
        }
    }

    open fun genArg(expression: IrExpression, codegen: ExpressionCodegen, index: Int, data: BlockInfo) {
        codegen.gen(expression, argsTypes[index], data)
    }

    companion object {
        fun create(
            expression: IrMemberAccessExpression,
            signature: JvmMethodSignature,
            context: JvmBackendContext,
            argsTypes: List<Type> = expression.argTypes(context),
            invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Unit
        ): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {

                override fun genInvokeInstruction(v: InstructionAdapter) = invokeInstruction(v)
            }
        }

        fun createWithResult(expression: IrMemberAccessExpression,
                   signature: JvmMethodSignature,
                   context: JvmBackendContext,
                   argsTypes: List<Type> = expression.argTypes(context),
                   invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Type): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {

                override fun genInvokeInstructionWithResult(v: InstructionAdapter) = invokeInstruction(v)
            }
        }

        fun create(
            expression: IrMemberAccessExpression,
            signature: JvmMethodSignature,
            context: JvmBackendContext,
            type: Type,
            invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Unit
        ): IrIntrinsicFunction {
            return create(expression, signature, context, listOf(type), invokeInstruction)
        }
    }
}

fun IrMemberAccessExpression.argTypes(context: JvmBackendContext): ArrayList<Type> {
    val callableMethod = context.state.typeMapper.mapToCallableMethod(descriptor as FunctionDescriptor, false)
    return arrayListOf<Type>().apply {
        callableMethod.dispatchReceiverType?.let { add(it) }
        addAll(callableMethod.getAsmMethod().argumentTypes)
    }
}

fun IrMemberAccessExpression.receiverAndArgs(): List<IrExpression> {
    return (arrayListOf(this.dispatchReceiver, this.extensionReceiver) +
            descriptor.valueParameters.mapIndexed { i, _ -> getValueArgument(i) }).filterNotNull()
}

fun List<IrExpression>.asmTypes(context: JvmBackendContext): List<Type> {
    return map { context.state.typeMapper.mapType(it.type.toKotlinType()) }
}