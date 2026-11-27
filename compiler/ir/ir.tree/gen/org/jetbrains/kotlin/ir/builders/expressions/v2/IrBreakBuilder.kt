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
import org.jetbrains.kotlin.ir.expressions.IrBreak
import org.jetbrains.kotlin.ir.expressions.impl.IrBreakImpl

class IrBreakBuilder @PublishedApi internal constructor() : IrBreakContinueBuilder()

@PublishedApi
internal fun build(builder: IrBreakBuilder): IrBreak {
    val node = IrBreakImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        loop = builder.loop,
    )
    node.label = builder.label
    return node
}

inline fun buildBreak(builder: IrBreakBuilder.() -> Unit): IrBreak {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrBreakBuilder().apply(builder))
}

inline fun IrBuilder.irBreak(builder: IrBreakBuilder.() -> Unit): IrBreak {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(breakBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.breakBuilder(): IrBreakBuilder =
    IrBreakBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.nothingType
    }
