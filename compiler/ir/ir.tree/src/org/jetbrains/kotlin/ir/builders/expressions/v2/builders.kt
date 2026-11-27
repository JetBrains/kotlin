/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.expressions.v2

import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun IrBuilder.irFunctionAccessExpression(
    symbol: IrFunctionSymbol,
    builder: IrFunctionAccessExpressionBuilder.() -> Unit = {},
): IrFunctionAccessExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return when (symbol) {
        is IrSimpleFunctionSymbol -> irCall(symbol, builder)
        is IrConstructorSymbol -> irConstructorCall(symbol, builder)
    }
}
