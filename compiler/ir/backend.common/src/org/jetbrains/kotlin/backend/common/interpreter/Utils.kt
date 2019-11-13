/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.Frame
import org.jetbrains.kotlin.backend.common.interpreter.stack.InterpreterFrame
import org.jetbrains.kotlin.backend.common.interpreter.stack.Primitive
import org.jetbrains.kotlin.backend.common.interpreter.stack.State
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.types.TypeUtils

fun IrInterpreter.calculateOverridden(symbol: IrFunctionSymbol, data: Frame): State {
    val owner = symbol.owner as IrFunctionImpl
    val overridden = owner.overriddenSymbols.first()
    val overriddenReceiver = data.getVar(symbol.getThisAsReceiver()).getState(overridden.getThisAsReceiver())
    val valueParameters = symbol.owner.valueParameters.zip(overridden.owner.valueParameters)
        .map { data.getVar(it.first.descriptor).setDescriptor(it.second.descriptor) }
    val newStates = InterpreterFrame((valueParameters + overriddenReceiver).toMutableList())

    return if (overridden.owner.body != null) {
        overridden.owner.body!!.accept(this, newStates)
    } else {
        calculateOverridden(overridden.owner.symbol, newStates)
    }
}

fun calculateBuiltIns(descriptor: FunctionDescriptor, frame: Frame): Any {
    val methodName = descriptor.name.asString()
    val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
    val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
    val argsValues = frame.getAll()
        .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
        .map { it.getIrConst().value }
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
            function.invoke(argsValues[1], argsValues[0])
        }
        else -> throw UnsupportedOperationException("Unsupported number of arguments")
    }
}

fun IrInterpreter.convertValueParameters(memberAccess: IrMemberAccessExpression, data: Frame): MutableList<State> {
    return mutableListOf<State>().apply {
        for (i in 0 until memberAccess.valueArgumentsCount) {
            val arg = memberAccess.getValueArgument(i)?.accept(this@convertValueParameters, data)
            arg?.setDescriptor((memberAccess.symbol.descriptor as FunctionDescriptor).valueParameters[i])?.let { this += it }
        }
    }
}

fun IrFunctionSymbol.getThisAsReceiver(): ReceiverParameterDescriptor {
    return (this.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionAccessExpression.getBody(): IrBody? {
    return this.symbol.owner.body
}

fun Any?.toIrConst(expression: IrExpression): IrConst<*> {
    return when (this) {
        is Boolean -> expression.copyParametersTo(IrConstKind.Boolean, this)
        is Char -> expression.copyParametersTo(IrConstKind.Char, this)
        is Byte -> expression.copyParametersTo(IrConstKind.Byte, this)
        is Short -> expression.copyParametersTo(IrConstKind.Short, this)
        is Int -> expression.copyParametersTo(IrConstKind.Int, this)
        is Long -> expression.copyParametersTo(IrConstKind.Long, this)
        is String -> expression.copyParametersTo(IrConstKind.String, this)
        is Float -> expression.copyParametersTo(IrConstKind.Float, this)
        is Double -> expression.copyParametersTo(IrConstKind.Double, this)
        null -> expression.copyParametersTo(IrConstKind.Null, this)
        else -> throw UnsupportedOperationException("Unsupported const element type $this")
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T> IrExpression.copyParametersTo(kind: IrConstKind<T>, value: Any?): IrConst<T> {
    return IrConstImpl(startOffset, endOffset, type, kind, value as T)
}

fun <T> IrConst<T>.toPrimitive(): Primitive<T> {
    return Primitive(this)
}