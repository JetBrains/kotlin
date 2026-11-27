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
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl

class IrWhenBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    var origin: IrStatementOrigin? = null

    val branches: MutableList<IrBranch> = []
}

@PublishedApi
internal fun build(builder: IrWhenBuilder): IrWhen {
    val node = IrWhenImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        origin = builder.origin,
    )
    node.branches.addAll(builder.branches)
    return node
}

inline fun buildWhen(builder: IrWhenBuilder.() -> Unit): IrWhen {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrWhenBuilder().apply(builder))
}

inline fun IrBuilder.irWhen(builder: IrWhenBuilder.() -> Unit): IrWhen {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(whenBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.whenBuilder(): IrWhenBuilder =
    IrWhenBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
