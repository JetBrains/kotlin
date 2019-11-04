/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.TypeUtils

class IrInterpreter : IrElementVisitor<IrExpression, Nothing?> {
    fun interpret(expression: IrExpression): IrExpression {
        return visitExpression(expression, null)
    }

    private fun calculateBuiltIns(descriptor: FunctionDescriptor, args: List<IrExpression>): Any {
        val methodName = descriptor.name.asString()
        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
        val argsValues = args.map { (it as? IrConst<*>)?.value ?: throw IllegalArgumentException("Builtin functions accept only const args") }
        val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })
        return when (argsType.size) {
            1 -> {
                val function = unaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in unary map")
                function.invoke(argsValues.first())
            }
            2 -> {
                val function = binaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in binary map")
                function.invoke(argsValues[0], argsValues[1])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
        }
    }

    private fun Any.toIrConst(expression: IrExpression): IrConst<*> {
        return when (this) {
            is Boolean -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Boolean, this)
            is Char -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Char, this)
            is Byte -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Byte, this)
            is Short -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Short, this)
            is Int -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Int, this)
            is Long -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Long, this)
            is String -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.String, this)
            is Float -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Float, this)
            is Double -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Double, this)
            else -> throw UnsupportedOperationException("Unsupported const element type $this")
        }
    }

    override fun visitElement(element: IrElement, data: Nothing?): IrExpression {
        return when (element) {
            is IrCall -> visitCall(element, data)
            else -> TODO("not supported")
        }
    }

    override fun visitCall(expression: IrCall, data: Nothing?): IrExpression {
        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)
        val receiverValue = dispatchReceiver ?: extensionReceiver
        val args = mutableListOf<IrExpression>()
        for (i in 0 until expression.valueArgumentsCount) {
            expression.getValueArgument(i)?.accept(this, data)?.also { args += it }
        }

        if (expression.symbol.owner.body == null) {
            return calculateBuiltIns(expression.symbol.descriptor, args + listOfNotNull(receiverValue)).toIrConst(expression)
        }

        return super.visitCall(expression, data)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Nothing?): IrExpression {
        return expression
    }
}