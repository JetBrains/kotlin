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
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.util.defaultType

class IrGetObjectValueBuilder @PublishedApi internal constructor(
    override val symbol: IrClassSymbol,
) : IrGetSingletonValueBuilder() {
    init {
        type = symbol.owner.defaultType
    }
}

@PublishedApi
internal fun build(builder: IrGetObjectValueBuilder): IrGetObjectValue {
    val node = IrGetObjectValueImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
    )
    return node
}

inline fun buildGetObjectValue(symbol: IrClassSymbol, builder: IrGetObjectValueBuilder.() -> Unit = {}): IrGetObjectValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrGetObjectValueBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irGetObjectValue(symbol: IrClassSymbol, builder: IrGetObjectValueBuilder.() -> Unit = {}): IrGetObjectValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(getObjectValueBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.getObjectValueBuilder(symbol: IrClassSymbol): IrGetObjectValueBuilder =
    IrGetObjectValueBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
