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
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass

class IrGetEnumValueBuilder @PublishedApi internal constructor(
    override val symbol: IrEnumEntrySymbol,
) : IrGetSingletonValueBuilder() {
    init {
        type = symbol.owner.parentAsClass.defaultType
    }
}

@PublishedApi
internal fun build(builder: IrGetEnumValueBuilder): IrGetEnumValue {
    val node = IrGetEnumValueImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
    )
    return node
}

inline fun buildGetEnumValue(symbol: IrEnumEntrySymbol, builder: IrGetEnumValueBuilder.() -> Unit = {}): IrGetEnumValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrGetEnumValueBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irGetEnumValue(symbol: IrEnumEntrySymbol, builder: IrGetEnumValueBuilder.() -> Unit = {}): IrGetEnumValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(getEnumValueBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.getEnumValueBuilder(symbol: IrEnumEntrySymbol): IrGetEnumValueBuilder =
    IrGetEnumValueBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
