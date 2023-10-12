/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.expressions

import org.jetbrains.kotlin.bir.symbols.BirSymbol
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.transformInPlace
import org.jetbrains.kotlin.bir.visitors.BirElementTransformer
import org.jetbrains.kotlin.bir.visitors.BirElementVisitor
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin

/**
 * A non-leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.memberAccessExpression]
 */
abstract class BirMemberAccessExpression<S : BirSymbol> : BirDeclarationReference() {
    var dispatchReceiver: BirExpression? = null

    var extensionReceiver: BirExpression? = null

    abstract override val symbol: S

    abstract var origin: IrStatementOrigin?

    protected abstract val valueArguments: Array<BirExpression?>

    protected abstract val typeArguments: Array<BirType?>

    val valueArgumentsCount: Int
        get() = valueArguments.size

    val typeArgumentsCount: Int
        get() = typeArguments.size

    override fun <D> acceptChildren(visitor: BirElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        valueArguments.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: BirElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
        valueArguments.transformInPlace(transformer, data)
    }

    fun getValueArgument(index: Int): BirExpression? {
        checkArgumentSlotAccess("value", index, valueArguments.size)
        return valueArguments[index]
    }

    fun getTypeArgument(index: Int): BirType? {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        return typeArguments[index]
    }

    fun putValueArgument(index: Int, valueArgument: BirExpression?) {
        checkArgumentSlotAccess("value", index, valueArguments.size)
        valueArguments[index] = valueArgument
    }

    fun putTypeArgument(index: Int, type: BirType?) {
        checkArgumentSlotAccess("type", index, typeArguments.size)
        typeArguments[index] = type
    }
}
