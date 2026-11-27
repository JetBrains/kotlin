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
import org.jetbrains.kotlin.ir.expressions.IrReturnableBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnableBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol

class IrReturnableBlockBuilder @PublishedApi internal constructor(
    val symbol: IrReturnableBlockSymbol,
) : IrBlockBuilder()

@PublishedApi
internal fun build(builder: IrReturnableBlockBuilder): IrReturnableBlock {
    val node = IrReturnableBlockImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        symbol = builder.symbol,
    )
    node.statements.addAll(builder.statements)
    return node
}

inline fun buildReturnableBlock(symbol: IrReturnableBlockSymbol, builder: IrReturnableBlockBuilder.() -> Unit): IrReturnableBlock {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrReturnableBlockBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irReturnableBlock(symbol: IrReturnableBlockSymbol, builder: IrReturnableBlockBuilder.() -> Unit): IrReturnableBlock {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(returnableBlockBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.returnableBlockBuilder(symbol: IrReturnableBlockSymbol): IrReturnableBlockBuilder =
    IrReturnableBlockBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
