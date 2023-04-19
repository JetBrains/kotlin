/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.utils.SmartList

class IrConstantPrimitiveImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var value: IrConst<*>,
) : IrConstantPrimitive() {
    override fun contentEquals(other: IrConstantValue) =
        other is IrConstantPrimitive &&
                type == other.type &&
                value.type == other.value.type &&
                value.kind == other.value.kind &&
                value.value == other.value.value

    override fun contentHashCode(): Int {
        var result = type.hashCode()
        result = result * 31 + value.type.hashCode()
        result = result * 31 + value.kind.hashCode()
        result = result * 31 + value.value.hashCode()
        return result
    }

    override var type = value.type
}

class IrConstantObjectImpl constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var constructor: IrConstructorSymbol,
    initValueArguments: List<IrConstantValue>,
    initTypeArguments: List<IrType>,
    override var type: IrType = constructor.owner.constructedClassType,
) : IrConstantObject() {
    override val valueArguments = SmartList(initValueArguments)
    override val typeArguments = SmartList(initTypeArguments)

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantObject &&
                other.type == type &&
                other.constructor == constructor &&
                valueArguments.size == other.valueArguments.size &&
                typeArguments.size == other.typeArguments.size &&
                valueArguments.indices.all { index -> valueArguments[index].contentEquals(other.valueArguments[index]) } &&
                typeArguments.indices.all { index -> typeArguments[index] == other.typeArguments[index] }


    override fun contentHashCode(): Int {
        var res = type.hashCode() * 31 + constructor.hashCode()
        for (value in valueArguments) {
            res = res * 31 + value.contentHashCode()
        }
        for (value in typeArguments) {
            res = res * 31 + value.hashCode()
        }
        return res
    }
}

class IrConstantArrayImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    initElements: List<IrConstantValue>,
) : IrConstantArray() {
    override val elements = SmartList(initElements)

    override fun contentEquals(other: IrConstantValue): Boolean =
        other is IrConstantArray &&
                other.type == type &&
                elements.size == other.elements.size &&
                elements.indices.all { elements[it].contentEquals(other.elements[it]) }

    override fun contentHashCode(): Int {
        var res = type.hashCode()
        for (value in elements) {
            res = res * 31 + value.contentHashCode()
        }
        return res
    }
}
