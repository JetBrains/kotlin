/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.stack.Complex
import org.jetbrains.kotlin.backend.common.interpreter.stack.Primitive
import org.jetbrains.kotlin.backend.common.interpreter.stack.State
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

// main purpose is to get receiver from constructor call
fun IrMemberAccessExpression.getThisAsReceiver(): DeclarationDescriptor {
    return (this.symbol.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionSymbol.getDispatchReceiver(): DeclarationDescriptor? {
    return (this.descriptor.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
}

fun IrFunctionSymbol.getExtensionReceiver(): DeclarationDescriptor? {
    return this.owner.extensionReceiverParameter?.descriptor
}

fun IrFunctionSymbol.getReceiver(): DeclarationDescriptor? {
    return this.getDispatchReceiver() ?: this.getExtensionReceiver()
}

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
    return this is VariableDescriptor && other is VariableDescriptor && this.name == other.name
}

fun IrCall.isAbstract(): Boolean {
    return (this.symbol.owner as? IrSimpleFunction)?.modality == Modality.ABSTRACT
}

fun IrCall.isFakeOverridden(): Boolean {
    return this.symbol.owner.isFakeOverride
}

fun IrFunction.isAbstract(): Boolean {
    return (this.symbol.owner as? IrSimpleFunction)?.modality == Modality.ABSTRACT
}

fun IrFunction.isFakeOverridden(): Boolean {
    return this.symbol.owner.isFakeOverride
}

fun State.toIrExpression(irBuiltIns: IrBuiltIns, expression: IrExpression): IrExpression {
    return when (this) {
        is Primitive<*> -> this.getValue().toIrConst(   // this is necessary to replace ir offsets
            irBuiltIns, expression.startOffset, expression.endOffset
        )
        else -> TODO("not supported")
    }
}

fun Any?.toState(irBuiltIns: IrBuiltIns): State {
    return when (this) {
        is Complex -> this
        else -> this.toIrConst(irBuiltIns).toPrimitive()
    }
}

fun Any?.toIrConst(irBuiltIns: IrBuiltIns, startOffset: Int = UNDEFINED_OFFSET, endOffset: Int = UNDEFINED_OFFSET): IrConst<*> {
    return when (this) {
        is Boolean -> IrConstImpl(startOffset, endOffset, irBuiltIns.booleanType, IrConstKind.Boolean, this)
        is Char -> IrConstImpl(startOffset, endOffset, irBuiltIns.charType, IrConstKind.Char, this)
        is Byte -> IrConstImpl(startOffset, endOffset, irBuiltIns.byteType, IrConstKind.Byte, this)
        is Short -> IrConstImpl(startOffset, endOffset, irBuiltIns.shortType, IrConstKind.Short, this)
        is Int -> IrConstImpl(startOffset, endOffset, irBuiltIns.intType, IrConstKind.Int, this)
        is Long -> IrConstImpl(startOffset, endOffset, irBuiltIns.longType, IrConstKind.Long, this)
        is String -> IrConstImpl(startOffset, endOffset, irBuiltIns.stringType, IrConstKind.String, this)
        is Float -> IrConstImpl(startOffset, endOffset, irBuiltIns.floatType, IrConstKind.Float, this)
        is Double -> IrConstImpl(startOffset, endOffset, irBuiltIns.doubleType, IrConstKind.Double, this)
        null -> IrConstImpl(startOffset, endOffset, irBuiltIns.nothingType, IrConstKind.Null, this)
        else -> throw UnsupportedOperationException("Unsupported const element type $this")
    }
}

fun <T> IrConst<T>.toPrimitive(): Primitive<T> {
    return Primitive(this.value, this.type)
}

fun IrAnnotationContainer.hasAnnotation(annotation: FqName): Boolean {
    if (this.annotations.isNotEmpty()) {
        return this.annotations.any { it.symbol.descriptor.containingDeclaration.fqNameSafe == annotation }
    }
    return false
}

fun IrAnnotationContainer.getAnnotation(annotation: FqName): IrConstructorCall {
    return this.annotations.first { it.symbol.descriptor.containingDeclaration.fqNameSafe == annotation }
}

fun getPrimitiveClass(fqName: String): Class<*>? {
    return when (fqName) {
        "kotlin.Boolean" -> Boolean::class.java
        "kotlin.Char" -> Char::class.java
        "kotlin.Byte" -> Byte::class.java
        "kotlin.Short" -> Short::class.java
        "kotlin.Int" -> Int::class.java
        "kotlin.Long" -> Long::class.java
        "kotlin.String" -> String::class.java
        "kotlin.Float" -> Float::class.java
        "kotlin.Double" -> Double::class.java
        else -> null
    }
}

fun IrType.getFqName(): String? {
    return this.classOrNull?.owner?.fqNameForIrSerialization?.asString()
}