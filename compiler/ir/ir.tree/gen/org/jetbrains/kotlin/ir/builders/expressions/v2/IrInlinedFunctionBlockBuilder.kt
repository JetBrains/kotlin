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
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.impl.IrInlinedFunctionBlockImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class IrInlinedFunctionBlockBuilder @PublishedApi internal constructor() : IrBlockBuilder() {
    var inlinedFunctionStartOffset: Int = UNDEFINED_OFFSET

    var inlinedFunctionEndOffset: Int = UNDEFINED_OFFSET

    var inlinedFunctionSymbol: IrFunctionSymbol? = null

    lateinit var inlinedFunctionFileEntry: IrFileEntry
}

@PublishedApi
internal fun build(builder: IrInlinedFunctionBlockBuilder): IrInlinedFunctionBlock {
    val node = IrInlinedFunctionBlockImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
        inlinedFunctionStartOffset = builder.inlinedFunctionStartOffset,
        inlinedFunctionEndOffset = builder.inlinedFunctionEndOffset,
        inlinedFunctionSymbol = builder.inlinedFunctionSymbol,
        inlinedFunctionFileEntry = builder.inlinedFunctionFileEntry,
    )
    node.statements.addAll(builder.statements)
    return node
}

inline fun buildInlinedFunctionBlock(builder: IrInlinedFunctionBlockBuilder.() -> Unit): IrInlinedFunctionBlock {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrInlinedFunctionBlockBuilder().apply(builder))
}

inline fun IrBuilder.irInlinedFunctionBlock(builder: IrInlinedFunctionBlockBuilder.() -> Unit): IrInlinedFunctionBlock {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(inlinedFunctionBlockBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.inlinedFunctionBlockBuilder(): IrInlinedFunctionBlockBuilder =
    IrInlinedFunctionBlockBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
