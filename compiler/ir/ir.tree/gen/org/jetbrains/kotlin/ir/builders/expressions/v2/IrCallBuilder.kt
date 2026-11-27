/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.expressions.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

class IrCallBuilder @PublishedApi internal constructor(
    override val symbol: IrSimpleFunctionSymbol,
) : IrFunctionAccessExpressionBuilder() {
    var superQualifierSymbol: IrClassSymbol? = null

    init {
        type = symbol.owner.returnType
        initializeShape(this)
    }
}

@PublishedApi
internal fun build(builder: IrCallBuilder): IrCall {
    val node = IrCallImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
        superQualifierSymbol = builder.superQualifierSymbol,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildCall(symbol: IrSimpleFunctionSymbol, builder: IrCallBuilder.() -> Unit = {}): IrCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrCallBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irCall(symbol: IrSimpleFunctionSymbol, builder: IrCallBuilder.() -> Unit = {}): IrCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(callBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.callBuilder(symbol: IrSimpleFunctionSymbol): IrCallBuilder =
    IrCallBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
