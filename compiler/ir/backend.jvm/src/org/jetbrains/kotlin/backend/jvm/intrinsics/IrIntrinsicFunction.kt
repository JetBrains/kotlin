/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*

open class IrIntrinsicFunction(
    val expression: IrFunctionAccessExpression,
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

    open fun invoke(
        v: InstructionAdapter,
        codegen: ExpressionCodegen,
        data: BlockInfo,
        expression: IrFunctionAccessExpression
    ): StackValue {
        loadArguments(codegen, data)
        codegen.markLineNumber(expression)
        return StackValue.onStack(genInvokeInstructionWithResult(v))
    }

    private fun loadArguments(codegen: ExpressionCodegen, data: BlockInfo) {
        var offset = 0
        expression.dispatchReceiver?.let { genArg(it, codegen, offset++, data) }
        expression.extensionReceiver?.let { genArg(it, codegen, offset++, data) }
        for ((i, valueParameter) in expression.symbol.owner.valueParameters.withIndex()) {
            val argument = expression.getValueArgument(i)
            when {
                argument != null ->
                    genArg(argument, codegen, i + offset, data)
                valueParameter.isVararg -> {
                    // TODO: is there an easier way to get the substituted type of an empty vararg argument?
                    val arrayType = codegen.typeMapper.mapType(
                        valueParameter.type.substitute(expression.symbol.owner.typeParameters, expression.typeArguments)
                    )
                    StackValue.operation(arrayType) {
                        it.aconst(0)
                        it.newarray(AsmUtil.correctElementType(arrayType))
                    }.put(arrayType, codegen.mv)
                }
                else -> error("Unknown parameter ${valueParameter.name} in: ${expression.dump()}")
            }
        }
    }

    private fun genArg(expression: IrExpression, codegen: ExpressionCodegen, index: Int, data: BlockInfo) {
        codegen.gen(expression, argsTypes[index], expression.type, data)
    }

    private val IrFunctionAccessExpression.typeArguments: List<IrType>
        get() = (0 until typeArgumentsCount).map { getTypeArgument(it)!! }

    companion object {
        fun create(
            expression: IrFunctionAccessExpression,
            signature: JvmMethodSignature,
            context: JvmBackendContext,
            argsTypes: List<Type> = expression.argTypes(context),
            invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Unit
        ): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {

                override fun genInvokeInstruction(v: InstructionAdapter) = invokeInstruction(v)
            }
        }

        fun createWithResult(
            expression: IrFunctionAccessExpression, signature: JvmMethodSignature,
            context: JvmBackendContext,
            argsTypes: List<Type> = expression.argTypes(context),
            invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Type
        ): IrIntrinsicFunction {
            return object : IrIntrinsicFunction(expression, signature, context, argsTypes) {

                override fun genInvokeInstructionWithResult(v: InstructionAdapter) = invokeInstruction(v)
            }
        }

        fun create(
            expression: IrFunctionAccessExpression,
            signature: JvmMethodSignature,
            context: JvmBackendContext,
            type: Type,
            invokeInstruction: IrIntrinsicFunction.(InstructionAdapter) -> Unit
        ): IrIntrinsicFunction {
            return create(expression, signature, context, listOf(type), invokeInstruction)
        }
    }
}

fun IrFunctionAccessExpression.argTypes(context: JvmBackendContext): ArrayList<Type> {
    val callee = symbol.owner
    val signature = context.methodSignatureMapper.mapSignatureSkipGeneric(callee)
    return arrayListOf<Type>().apply {
        if (dispatchReceiver != null) {
            add(context.typeMapper.mapClass(callee.parentAsClass))
        }
        addAll(signature.asmMethod.argumentTypes)
    }
}
