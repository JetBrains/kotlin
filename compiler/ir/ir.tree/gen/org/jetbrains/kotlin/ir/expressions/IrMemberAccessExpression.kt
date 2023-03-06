/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.memberAccessExpression]
 */
abstract class IrMemberAccessExpression<S : IrSymbol> : IrDeclarationReference() {
    var dispatchReceiver: IrExpression? = null

    var extensionReceiver: IrExpression? = null

    abstract override val symbol: S

    abstract var origin: IrStatementOrigin?

    protected abstract val valueArguments: Array<IrExpression?>

    protected abstract val typeArguments: Array<IrType?>

    val valueArgumentsCount: Int
        get() = valueArguments.size

    val typeArgumentsCount: Int
        get() = typeArguments.size

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        valueArguments.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
        valueArguments.transformInPlace(transformer, data)
    }

    fun getValueArgument(index: Int): IrExpression? {
        checkArgumentSlotAccess("value", index, valueArguments.size)
        return valueArguments[index]
    }

    fun getTypeArgument(index: Int): IrType? {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        return typeArguments[index]
    }

    fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        checkArgumentSlotAccess("value", index, valueArguments.size)
        valueArguments[index] = valueArgument
    }

    fun putTypeArgument(index: Int, type: IrType?) {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        typeArguments[index] = type
    }
}
