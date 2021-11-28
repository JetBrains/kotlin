/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.SmartList

class IrConstantPrimitiveImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var value: IrConst<*>,
) : IrConstantPrimitive() {
    override fun contentEquals(other: IrConstantValue) =
        other is IrConstantPrimitive &&
                value.type == other.value.type &&
                value.kind == other.value.kind &&
                value.value == other.value

    override fun contentHashCode() =
        (value.type.hashCode() * 31 + value.kind.hashCode()) * 31 + value.value.hashCode()

    override var type = value.type

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        value.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        value = value.transform(transformer, data) as IrConst<*>
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantPrimitive(this, data)
    }
}

class IrConstantObjectImpl constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override val constructor: IrConstructorSymbol,
    initArguments: List<IrConstantValue>,
    override val typeArguments: List<IrType>,
    override var type: IrType = constructor.owner.constructedClassType,
) : IrConstantObject() {
    override val valueArguments = SmartList(initArguments)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantObject(this, data)
    }

    override fun putArgument(index: Int, value: IrConstantValue) {
        valueArguments[index] = value
    }

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

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        valueArguments.forEach { value -> value.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        valueArguments.transformInPlace { it.transform(transformer, data) }
    }
}

class IrConstantArrayImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    initElements: List<IrConstantValue>,
) : IrConstantArray() {
    override val elements = SmartList(initElements)
    override fun putElement(index: Int, value: IrConstantValue) {
        elements[index] = value
    }

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

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstantArray(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        elements.forEach { value -> value.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        elements.transformInPlace { value -> value.transform(transformer, data) }
    }
}
