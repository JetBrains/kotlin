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
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.impl.IrSpreadElementImpl

class IrSpreadElementBuilder @PublishedApi internal constructor() : IrElementBuilder() {
    lateinit var expression: IrExpression
}

@PublishedApi
internal fun build(builder: IrSpreadElementBuilder): IrSpreadElement {
    val node = IrSpreadElementImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        expression = builder.expression,
    )
    return node
}

inline fun buildSpreadElement(builder: IrSpreadElementBuilder.() -> Unit): IrSpreadElement {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSpreadElementBuilder().apply(builder))
}

inline fun IrBuilder.irSpreadElement(builder: IrSpreadElementBuilder.() -> Unit): IrSpreadElement {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(spreadElementBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.spreadElementBuilder(): IrSpreadElementBuilder =
    IrSpreadElementBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
