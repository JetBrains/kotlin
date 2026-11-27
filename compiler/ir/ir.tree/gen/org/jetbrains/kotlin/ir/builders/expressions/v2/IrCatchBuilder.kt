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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCatchImpl

class IrCatchBuilder @PublishedApi internal constructor() : IrElementBuilder() {
    lateinit var catchParameter: IrVariable

    lateinit var result: IrExpression

    var origin: IrStatementOrigin? = null
}

@PublishedApi
internal fun build(builder: IrCatchBuilder): IrCatch {
    val node = IrCatchImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        catchParameter = builder.catchParameter,
        origin = builder.origin,
    )
    node.result = builder.result
    return node
}

inline fun buildCatch(builder: IrCatchBuilder.() -> Unit): IrCatch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrCatchBuilder().apply(builder))
}

inline fun IrBuilder.irCatch(builder: IrCatchBuilder.() -> Unit): IrCatch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(catchBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.catchBuilder(): IrCatchBuilder =
    IrCatchBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
