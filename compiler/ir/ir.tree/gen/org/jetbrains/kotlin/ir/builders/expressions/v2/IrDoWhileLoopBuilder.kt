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
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl

class IrDoWhileLoopBuilder @PublishedApi internal constructor() : IrLoopBuilder()

@PublishedApi
internal fun build(builder: IrDoWhileLoopBuilder): IrDoWhileLoop {
    val node = IrDoWhileLoopImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
    )
    node.body = builder.body
    node.condition = builder.condition
    node.label = builder.label
    return node
}

inline fun buildDoWhileLoop(builder: IrDoWhileLoopBuilder.() -> Unit): IrDoWhileLoop {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrDoWhileLoopBuilder().apply(builder))
}

inline fun IrBuilder.irDoWhileLoop(builder: IrDoWhileLoopBuilder.() -> Unit): IrDoWhileLoop {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(doWhileLoopBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.doWhileLoopBuilder(): IrDoWhileLoopBuilder =
    IrDoWhileLoopBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.unitType
    }
