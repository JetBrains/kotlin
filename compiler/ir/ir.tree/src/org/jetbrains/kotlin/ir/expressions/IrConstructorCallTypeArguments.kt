/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.types.IrType

class ConstructorTypeArguments(internal val irConstructorCall: IrConstructorCall) : AbstractList<IrType?>() {
    override val size: Int
        get() = irConstructorCall.constructorTypeArgumentsCount

    override fun get(index: Int): IrType? =
        if (index >= size)
            throw IndexOutOfBoundsException("index: $index, size: $size")
        else
            irConstructorCall.getConstructorTypeArgument(index)
}

class ClassTypeArguments(internal val irConstructorCall: IrConstructorCall) : AbstractList<IrType?>() {
    override val size: Int
        get() = irConstructorCall.classTypeArgumentsCount

    override fun get(index: Int): IrType? =
        if (index >= size)
            throw IndexOutOfBoundsException("index: $index, size: $size")
        else
            irConstructorCall.getTypeArgument(index)
}

fun IrConstructorCall.getConstructorTypeArgumentIndex(constructorTypeArgumentIndex: Int) =
    typeArgumentsCount - constructorTypeArgumentsCount + constructorTypeArgumentIndex

fun IrConstructorCall.getConstructorTypeArgument(index: Int): IrType? =
    getTypeArgument(getConstructorTypeArgumentIndex(index))

fun IrConstructorCall.putConstructorTypeArgument(index: Int, type: IrType?) {
    putTypeArgument(getConstructorTypeArgumentIndex(index), type)
}

operator fun ConstructorTypeArguments.set(index: Int, type: IrType?) {
    if (index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
    irConstructorCall.putConstructorTypeArgument(index, type)
}

val IrConstructorCall.classTypeArgumentsCount: Int
    get() = typeArgumentsCount - constructorTypeArgumentsCount

fun IrConstructorCall.getClassTypeArgument(index: Int): IrType? =
    getTypeArgument(index)

fun IrConstructorCall.putClassTypeArgument(index: Int, type: IrType?) {
    putTypeArgument(index, type)
}

operator fun ClassTypeArguments.set(index: Int, type: IrType?) {
    if (index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
    irConstructorCall.putClassTypeArgument(index, type)
}

fun IrConstructorCall.getConstructorTypeArguments() =
    ConstructorTypeArguments(this)

fun IrConstructorCall.getClassTypeArguments() =
    ClassTypeArguments(this)

val IrConstructorCall.outerClassReceiver: IrExpression?
    get() = if (symbol.owner.dispatchReceiverParameter != null) arguments[0] else null
