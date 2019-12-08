/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.stack.Complex
import org.jetbrains.kotlin.backend.common.interpreter.stack.Primitive
import org.jetbrains.kotlin.backend.common.interpreter.stack.State
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

// main purpose is to get receiver from constructor call
fun IrMemberAccessExpression.getThisAsReceiver(): DeclarationDescriptor {
    return (this.symbol.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionSymbol.getThisAsReceiver(): DeclarationDescriptor? {
    return (this.descriptor.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
        ?: this.owner.extensionReceiverParameter?.descriptor
}

/*fun IrFunctionSymbol.getReceiverDescriptor(): DeclarationDescriptor? {
    return this.owner.dispatchReceiverParameter?.descriptor ?: this.owner.extensionReceiverParameter?.descriptor
}*/

fun IrFunctionAccessExpression.getBody(): IrBody? {
    return this.symbol.owner.body
}

fun DeclarationDescriptor.equalTo(other: DeclarationDescriptor): Boolean {
    return this.isSubtypeOf(other) || this.hasSameNameAs(other) || this == other
}

private fun DeclarationDescriptor.isSubtypeOf(other: DeclarationDescriptor): Boolean {
    if (this !is ReceiverParameterDescriptor || other !is ReceiverParameterDescriptor) return false
    return when {
        this.value is ImplicitClassReceiver && other.value is ImplicitClassReceiver -> this.value.type.isSubtypeOf(other.value.type)
        this.value is ExtensionReceiver && other.value is ExtensionReceiver -> this.value == other.value
        else -> false
    }
}

private fun DeclarationDescriptor.hasSameNameAs(other: DeclarationDescriptor): Boolean {
    return this is ValueParameterDescriptor && other is ValueParameterDescriptor && this.name == other.name
}

fun IrCall.isAbstract(): Boolean {
    return (this.symbol.owner as? IrSimpleFunction)?.modality == Modality.ABSTRACT
}

fun IrCall.isFakeOverridden(): Boolean {
    return this.symbol.owner.isFakeOverride
}

fun State?.getIrFunction(expression: IrCall): IrFunction {
    return this.let { it?.getIrFunctionByName(expression.symbol.descriptor.name) } ?: expression.symbol.owner
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