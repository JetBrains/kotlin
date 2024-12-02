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
            irConstructorCall.typeArguments[index]
}

fun IrConstructorCall.getConstructorTypeArgumentIndex(constructorTypeArgumentIndex: Int) =
    typeArguments.size - constructorTypeArgumentsCount + constructorTypeArgumentIndex

fun IrConstructorCall.getConstructorTypeArgument(index: Int): IrType? =
    typeArguments[getConstructorTypeArgumentIndex(index)]

fun IrConstructorCall.putConstructorTypeArgument(index: Int, type: IrType?) {
    typeArguments[getConstructorTypeArgumentIndex(index)] = type
}

operator fun ConstructorTypeArguments.set(index: Int, type: IrType?) {
    if (index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
    irConstructorCall.putConstructorTypeArgument(index, type)
}

val IrConstructorCall.classTypeArgumentsCount: Int
    get() = typeArguments.size - constructorTypeArgumentsCount

fun IrConstructorCall.getClassTypeArgument(index: Int): IrType? =
    typeArguments[index]

fun IrConstructorCall.putClassTypeArgument(index: Int, type: IrType?) {
    typeArguments[index] = type
}

operator fun ClassTypeArguments.set(index: Int, type: IrType?) {
    if (index >= size) throw IndexOutOfBoundsException("index: $index, size: $size")
    irConstructorCall.putClassTypeArgument(index, type)
}

fun IrConstructorCall.getConstructorTypeArguments() =
    ConstructorTypeArguments(this)

fun IrConstructorCall.getClassTypeArguments() =
    ClassTypeArguments(this)

var IrConstructorCall.outerClassReceiver: IrExpression?
    get() = dispatchReceiver
    set(value) {
        dispatchReceiver = value
    }
