/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

// todo: autogenerate
/**
 * A non-leaf IR tree element.
 */
abstract class IrMemberAccessExpression<S : IrSymbol> : IrDeclarationReference() {
    var dispatchReceiver: IrExpression? = null
    var extensionReceiver: IrExpression? = null

    abstract override val symbol: S

    abstract val origin: IrStatementOrigin?

    protected abstract val typeArguments: Array<IrType?>
    val typeArgumentsCount: Int get() = typeArguments.size

    protected abstract val valueArguments: Array<IrExpression?>
    open val valueArgumentsCount: Int get() = valueArguments.size

    fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        return valueArguments[index]
    }

    fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        valueArguments[index] = valueArgument
    }

    fun getTypeArgument(index: Int): IrType? {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        return typeArguments[index]
    }

    fun putTypeArgument(index: Int, type: IrType?) {
        if (index >= typeArgumentsCount) {
            throwNoSuchArgumentSlotException("type", index, typeArgumentsCount)
        }
        typeArguments[index] = type
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        dispatchReceiver?.accept(visitor, data)
        extensionReceiver?.accept(visitor, data)
        if (valueArgumentsCount > 0) {
            valueArguments.forEach { it?.accept(visitor, data) }
        }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        dispatchReceiver = dispatchReceiver?.transform(transformer, data)
        extensionReceiver = extensionReceiver?.transform(transformer, data)
        if (valueArgumentsCount > 0) {
            valueArguments.forEachIndexed { i, irExpression ->
                valueArguments[i] = irExpression?.transform(transformer, data)
            }
        }
    }
}
