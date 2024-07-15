/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.utils.SmartList

/**
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.constantObject]
 */
abstract class IrConstantObject(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    var constructor: IrConstructorSymbol,
) : IrConstantValue(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
) {
    val valueArguments: MutableList<IrConstantValue> = SmartList()

    val typeArguments: MutableList<IrType> = SmartList()

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstantObject(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        valueArguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        valueArguments.transformInPlace(transformer, data)
    }
}
