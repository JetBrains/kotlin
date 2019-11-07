/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.stack.Primitive
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

fun IrFunctionSymbol.getThisAsReceiver(): ReceiverParameterDescriptor {
    return (this.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter
}

fun IrFunctionAccessExpression.getBody(): IrBody? {
    return this.symbol.owner.body
}

fun Any.toIrConst(expression: IrExpression): IrConst<*> {
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

fun <T> IrConst<T>.toPrimitive(): Primitive<T> {
    return Primitive(this)
}