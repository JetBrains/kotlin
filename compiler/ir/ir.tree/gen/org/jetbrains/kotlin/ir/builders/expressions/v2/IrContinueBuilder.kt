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
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.impl.IrContinueImpl

class IrContinueBuilder @PublishedApi internal constructor() : IrBreakContinueBuilder()

@PublishedApi
internal fun build(builder: IrContinueBuilder): IrContinue {
    val node = IrContinueImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        loop = builder.loop,
    )
    node.label = builder.label
    return node
}

inline fun buildContinue(builder: IrContinueBuilder.() -> Unit): IrContinue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrContinueBuilder().apply(builder))
}

inline fun IrBuilder.irContinue(builder: IrContinueBuilder.() -> Unit): IrContinue {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(continueBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.continueBuilder(): IrContinueBuilder =
    IrContinueBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.nothingType
    }
