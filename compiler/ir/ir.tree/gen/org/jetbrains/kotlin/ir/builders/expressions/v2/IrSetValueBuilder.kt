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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol

class IrSetValueBuilder @PublishedApi internal constructor(
    override val symbol: IrValueSymbol,
) : IrValueAccessExpressionBuilder() {
    lateinit var value: IrExpression
}

@PublishedApi
internal fun build(builder: IrSetValueBuilder): IrSetValue {
    val node = IrSetValueImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
        origin = builder.origin,
        value = builder.value,
    )
    return node
}

inline fun buildSetValue(symbol: IrValueSymbol, builder: IrSetValueBuilder.() -> Unit): IrSetValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSetValueBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irSetValue(symbol: IrValueSymbol, builder: IrSetValueBuilder.() -> Unit): IrSetValue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(setValueBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.setValueBuilder(symbol: IrValueSymbol): IrSetValueBuilder =
    IrSetValueBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.unitType
    }
