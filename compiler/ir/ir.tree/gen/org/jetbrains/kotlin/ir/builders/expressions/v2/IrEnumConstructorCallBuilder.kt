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
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol

class IrEnumConstructorCallBuilder @PublishedApi internal constructor(
    override val symbol: IrConstructorSymbol,
) : IrFunctionAccessExpressionBuilder() {
    init {
        initializeShape(this)
    }
}

@PublishedApi
internal fun build(builder: IrEnumConstructorCallBuilder): IrEnumConstructorCall {
    val node = IrEnumConstructorCallImpl(
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

inline fun buildEnumConstructorCall(symbol: IrConstructorSymbol, builder: IrEnumConstructorCallBuilder.() -> Unit): IrEnumConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrEnumConstructorCallBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irEnumConstructorCall(symbol: IrConstructorSymbol, builder: IrEnumConstructorCallBuilder.() -> Unit): IrEnumConstructorCall {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(enumConstructorCallBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.enumConstructorCallBuilder(symbol: IrConstructorSymbol): IrEnumConstructorCallBuilder =
    IrEnumConstructorCallBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
