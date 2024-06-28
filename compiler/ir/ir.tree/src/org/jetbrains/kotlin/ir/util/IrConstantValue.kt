/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.expressions.IrConstantArray
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.expressions.IrConstantValue

fun IrConstantValue.contentEquals(other: IrConstantValue): Boolean = when (this) {
    is IrConstantPrimitive -> contentEquals(other)
    is IrConstantObject -> contentEquals(other)
    is IrConstantArray -> contentEquals(other)
}

fun IrConstantValue.contentHashCode(): Int = when (this) {
    is IrConstantPrimitive -> contentHashCode()
    is IrConstantObject -> contentHashCode()
    is IrConstantArray -> contentHashCode()
}


fun IrConstantPrimitive.contentEquals(other: IrConstantValue): Boolean =
    other is IrConstantPrimitive &&
            type == other.type &&
            value.type == other.value.type &&
            value.kind == other.value.kind &&
            value.value == other.value.value

fun IrConstantPrimitive.contentHashCode(): Int {
    var result = type.hashCode()
    result = result * 31 + value.type.hashCode()
    result = result * 31 + value.kind.hashCode()
    result = result * 31 + value.value.hashCode()
    return result
}


fun IrConstantObject.contentEquals(other: IrConstantValue): Boolean =
    other is IrConstantObject &&
            other.type == type &&
            other.constructor == constructor &&
            valueArguments.size == other.valueArguments.size &&
            typeArguments.size == other.typeArguments.size &&
            valueArguments.indices.all { index -> valueArguments[index].contentEquals(other.valueArguments[index]) } &&
            typeArguments.indices.all { index -> typeArguments[index] == other.typeArguments[index] }

fun IrConstantObject.contentHashCode(): Int {
    var res = type.hashCode() * 31 + constructor.hashCode()
    for (value in valueArguments) {
        res = res * 31 + value.contentHashCode()
    }
    for (value in typeArguments) {
        res = res * 31 + value.hashCode()
    }
    return res
}


fun IrConstantArray.contentEquals(other: IrConstantValue): Boolean =
    other is IrConstantArray &&
            other.type == type &&
            elements.size == other.elements.size &&
            elements.indices.all { elements[it].contentEquals(other.elements[it]) }

fun IrConstantArray.contentHashCode(): Int {
    var res = type.hashCode()
    for (value in elements) {
        res = res * 31 + value.contentHashCode()
    }
    return res
}
