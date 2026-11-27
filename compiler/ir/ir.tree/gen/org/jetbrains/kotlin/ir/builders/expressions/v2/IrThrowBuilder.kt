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
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.expressions.impl.IrThrowImpl

class IrThrowBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var value: IrExpression
}

@PublishedApi
internal fun build(builder: IrThrowBuilder): IrThrow {
    val node = IrThrowImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        value = builder.value,
    )
    return node
}

inline fun buildThrow(builder: IrThrowBuilder.() -> Unit): IrThrow {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrThrowBuilder().apply(builder))
}

inline fun IrBuilder.irThrow(builder: IrThrowBuilder.() -> Unit): IrThrow {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(throwBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.throwBuilder(): IrThrowBuilder =
    IrThrowBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.nothingType
    }
