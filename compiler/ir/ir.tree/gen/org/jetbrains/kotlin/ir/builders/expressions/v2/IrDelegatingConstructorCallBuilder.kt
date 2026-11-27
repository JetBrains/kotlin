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
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol

class IrDelegatingConstructorCallBuilder @PublishedApi internal constructor(
    override val symbol: IrConstructorSymbol,
) : IrFunctionAccessExpressionBuilder() {
    init {
        initializeShape(this)
    }
}

@PublishedApi
internal fun build(builder: IrDelegatingConstructorCallBuilder): IrDelegatingConstructorCall {
    val node = IrDelegatingConstructorCallImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
    )
    initializeArguments(builder, node)
    return node
}

inline fun buildDelegatingConstructorCall(symbol: IrConstructorSymbol, builder: IrDelegatingConstructorCallBuilder.() -> Unit): IrDelegatingConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrDelegatingConstructorCallBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irDelegatingConstructorCall(symbol: IrConstructorSymbol, builder: IrDelegatingConstructorCallBuilder.() -> Unit = {}): IrDelegatingConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(delegatingConstructorCallBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.delegatingConstructorCallBuilder(symbol: IrConstructorSymbol): IrDelegatingConstructorCallBuilder =
    IrDelegatingConstructorCallBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.unitType
    }
