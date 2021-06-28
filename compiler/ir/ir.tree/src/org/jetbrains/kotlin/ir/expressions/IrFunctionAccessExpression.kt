/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrFunctionAccessExpression(
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
) : IrMemberAccessExpression<IrFunctionSymbol>(typeArgumentsCount) {
    private val argumentsByParameterIndex: Array<IrExpression?> = arrayOfNulls(valueArgumentsCount)

    final override val valueArgumentsCount: Int
        get() = argumentsByParameterIndex.size

    var contextReceiversCount: Int = 0

    override fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throwNoSuchArgumentSlotException("value", index, valueArgumentsCount)
        }
        return argumentsByParameterIndex[index]
    }

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throwNoSuchArgumentSlotException("value", index, valueArgumentsCount)
        }
        argumentsByParameterIndex[index] = valueArgument
    }

    override fun removeValueArgument(index: Int) {
        argumentsByParameterIndex[index] = null
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        argumentsByParameterIndex.forEach { it?.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        super.transformChildren(transformer, data)
        argumentsByParameterIndex.forEachIndexed { i, irExpression ->
            argumentsByParameterIndex[i] = irExpression?.transform(transformer, data)
        }
    }
}

fun IrFunctionAccessExpression.putArgument(parameter: IrValueParameter, argument: IrExpression) =
    putArgument(symbol.owner, parameter, argument)
