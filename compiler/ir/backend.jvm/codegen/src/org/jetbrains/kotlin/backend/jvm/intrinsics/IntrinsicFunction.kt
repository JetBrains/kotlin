/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class IntrinsicFunction(
    val expression: IrFunctionAccessExpression,
    val signature: JvmMethodSignature,
    val classCodegen: ClassCodegen,
    val argsTypes: List<Type>,
) {
    abstract fun genInvokeInstruction(v: InstructionAdapter)

    open fun invoke(
        v: InstructionAdapter,
        codegen: ExpressionCodegen,
        data: BlockInfo,
        expression: IrFunctionAccessExpression,
    ): StackValue {
        loadArguments(codegen, data)
        with(codegen) { expression.markLineNumber(startOffset = true) }
        genInvokeInstruction(v)
        return StackValue.onStack(signature.returnType)
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
            classCodegen: ClassCodegen,
            argsTypes: List<Type> = expression.argTypes(classCodegen),
            invokeInstruction: IntrinsicFunction.(InstructionAdapter) -> Unit,
        ): IntrinsicFunction =
            object : IntrinsicFunction(expression, signature, classCodegen, argsTypes) {
                override fun genInvokeInstruction(v: InstructionAdapter) = invokeInstruction(v)
            }
    }
}

internal fun IrFunctionAccessExpression.argTypes(classCodegen: ClassCodegen): List<Type> {
    val callee = symbol.owner
    val signature = classCodegen.methodSignatureMapper.mapSignatureSkipGeneric(callee)
    return arrayListOf<Type>().apply {
        if (dispatchReceiver != null) {
            add(classCodegen.typeMapper.mapClass(callee.parentAsClass))
        }
        addAll(signature.asmMethod.argumentTypes)
    }
}
