/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

fun IrInterpreter.calculateAbstract(irFunction: IrFunction?, data: Frame): State {
    return irFunction?.body?.accept(this, data)
        ?: throw NoSuchMethodException("Method \"$irFunction\" wasn't implemented")
}

fun IrInterpreter.calculateOverridden(owner: IrFunctionImpl, data: Frame): State {
    val overridden = owner.overriddenSymbols.first()

    val variableDescriptor = owner.symbol.getReceiverDescriptor()!!
    val overriddenReceiver = overridden.getReceiverDescriptor()!!
    val overriddenReceiverState = data.getVariableState(variableDescriptor).getState(overriddenReceiver)
        ?: throw NoSuchElementException("Variable \"$variableDescriptor\" doesn't contains state \"$overriddenReceiver\"")

    val valueParameters = owner.valueParameters.zip(overridden.owner.valueParameters)
        .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
    val newStates = InterpreterFrame((valueParameters + Variable(overriddenReceiver, overriddenReceiverState)).toMutableList())

    var overriddenOwner: IrSimpleFunction? = overridden.owner
    do {
        val body = overriddenOwner?.body
        when {
            body != null -> return body.accept(this, newStates)
            else -> overriddenOwner = overriddenOwner?.overriddenSymbols?.firstOrNull()?.owner
        }
    } while (overriddenOwner != null)

    throw NoSuchMethodException("$owner has no body")
}

fun IrInterpreter.calculateBuiltIns(expression: IrCall, frame: Frame): Any {
    val descriptor = expression.symbol.descriptor
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
            when (methodName) {
                "rangeTo" -> calculateRangeTo(expression, frame)
                else -> function.invoke(argsValues[0], argsValues[1])
            }
        }
        else -> throw UnsupportedOperationException("Unsupported number of arguments")
    }
}

private fun IrInterpreter.calculateRangeTo(expression: IrExpression, data: Frame): Any {
    val constructor = expression.type.classOrNull!!.owner.constructors.first()
    val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

    val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
    primitiveValueParameters.forEachIndexed { index, primitive -> constructorCall.putValueArgument(index, primitive.getIrConst()) }

    val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
    val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

    return constructorCall.accept(this, newFrame)
}

fun IrInterpreter.convertValueParameters(memberAccess: IrMemberAccessExpression, data: Frame): MutableList<Variable> {
    return mutableListOf<Variable>().apply {
        for (i in 0 until memberAccess.valueArgumentsCount) {
            val arg = memberAccess.getValueArgument(i)?.accept(this@convertValueParameters, data)
            arg?.let { add(Variable((memberAccess.symbol.descriptor as FunctionDescriptor).valueParameters[i], it)) }
        }
    }
}

// main purpose is to get receiver from constructor call
fun IrFunctionAccessExpression.getThisAsReceiver(): DeclarationDescriptor {
    return (this.symbol.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionSymbol.getReceiverDescriptor(): DeclarationDescriptor? {
    return this.owner.dispatchReceiverParameter?.descriptor ?: this.owner.extensionReceiverParameter?.descriptor
}

fun IrFunctionAccessExpression.getBody(): IrBody? {
    return this.symbol.owner.body
}

fun DeclarationDescriptor.isSubtypeOf(other: DeclarationDescriptor): Boolean {
    if (this !is ReceiverParameterDescriptor || other !is ReceiverParameterDescriptor) return false
    return when {
        this.value is ImplicitClassReceiver && other.value is ImplicitClassReceiver -> this.value.type.isSubtypeOf(other.value.type)
        this.value is ExtensionReceiver && other.value is ExtensionReceiver -> this.value == other.value
        else -> false
    }
}

fun DeclarationDescriptor.hasSameNameAs(other: DeclarationDescriptor): Boolean {
    return this is ValueParameterDescriptor && other is ValueParameterDescriptor && this.name == other.name
}

fun IrCall.isAbstract(): Boolean {
    return (this.symbol.owner as? IrSimpleFunction)?.modality == Modality.ABSTRACT
}

fun IrCall.isFakeOverridden(): Boolean {
    return this.symbol.owner.isFakeOverride
}

fun State?.getIrFunction(expression: IrCall): IrFunction {
    return this.let { (it as? Complex)?.getIrFunctionByName(expression.symbol.descriptor.name) } ?: expression.symbol.owner
}

fun State.toIrExpression(expression: IrExpression): IrExpression {
    return when (this) {
        is Primitive<*> -> this.getIrConst().value.toIrConst(expression) // it is necessary to replace ir offsets
        else -> TODO("not supported")
    }
}

fun Any?.toState(expression: IrExpression): State {
    return when (this) {
        is Complex -> this
        else -> this.toIrConst(expression).toPrimitive()
    }
}

private fun Any?.toIrConst(expression: IrExpression): IrConst<*> {
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