/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.types.TypeUtils

fun IrInterpreter.calculateOverridden(symbol: IrFunctionSymbol, data: Frame): State {
    val owner = symbol.owner as IrFunctionImpl
    val overridden = owner.overriddenSymbols.first()
    val overriddenReceiver = data.getVariableState(symbol.getThisAsReceiver()).getState(overridden.getThisAsReceiver())
    val valueParameters = symbol.owner.valueParameters.zip(overridden.owner.valueParameters)
        .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
    val newStates = InterpreterFrame((valueParameters + Variable(overridden.getThisAsReceiver(), overriddenReceiver)).toMutableList())

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
        .map { it.state }
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
            function.invoke(argsValues[0], argsValues[1])
        }
        else -> throw UnsupportedOperationException("Unsupported number of arguments")
    }
}

fun IrInterpreter.calculateAbstract(irFunction: IrFunction?, data: Frame): State {
    return irFunction?.body?.accept(this, data)!!
}

fun IrInterpreter.convertValueParameters(memberAccess: IrMemberAccessExpression, data: Frame): MutableList<Variable> {
    return mutableListOf<Variable>().apply {
        for (i in 0 until memberAccess.valueArgumentsCount) {
            val arg = memberAccess.getValueArgument(i)?.accept(this@convertValueParameters, data)
            arg?.let { add(Variable((memberAccess.symbol.descriptor as FunctionDescriptor).valueParameters[i], it)) }
        }
    }
}

fun IrFunctionSymbol.getThisAsReceiver(): ReceiverParameterDescriptor {
    return (this.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionAccessExpression.getThisAsReceiver(): ReceiverParameterDescriptor {
    return (this.symbol.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionAccessExpression.getBody(): IrBody? {
    return this.symbol.owner.body
}

fun IrCall.isAbstract(): Boolean {
    return (this.symbol.owner as? IrSimpleFunction)?.modality == Modality.ABSTRACT
}

fun IrCall.isFakeOverridden(): Boolean {
    return this.symbol.owner.isFakeOverride
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