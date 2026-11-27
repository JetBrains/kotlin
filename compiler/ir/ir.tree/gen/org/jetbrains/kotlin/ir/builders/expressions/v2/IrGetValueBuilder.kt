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
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

class IrGetValueBuilder @PublishedApi internal constructor(
    override val symbol: IrValueSymbol,
) : IrValueAccessExpressionBuilder() {
    init {
        type = symbol.owner.type
    }
}

@PublishedApi
internal fun build(builder: IrGetValueBuilder): IrGetValue {
    val node = IrGetValueImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
        origin = builder.origin,
    )
    return node
}

inline fun buildGetValue(symbol: IrValueSymbol, builder: IrGetValueBuilder.() -> Unit = {}): IrGetValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrGetValueBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irGetValue(symbol: IrValueSymbol, builder: IrGetValueBuilder.() -> Unit = {}): IrGetValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(getValueBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.getValueBuilder(symbol: IrValueSymbol): IrGetValueBuilder =
    IrGetValueBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
