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
import org.jetbrains.kotlin.ir.expressions.IrErrorCallExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorCallExpressionImpl

class IrErrorCallExpressionBuilder @PublishedApi internal constructor() : IrErrorExpressionBuilder() {
    var explicitReceiver: IrExpression? = null

    val arguments: MutableList<IrExpression> = []
}

@PublishedApi
internal fun build(builder: IrErrorCallExpressionBuilder): IrErrorCallExpression {
    val node = IrErrorCallExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        description = builder.description,
    )
    node.explicitReceiver = builder.explicitReceiver
    node.arguments.addAll(builder.arguments)
    return node
}

inline fun buildErrorCallExpression(builder: IrErrorCallExpressionBuilder.() -> Unit): IrErrorCallExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrErrorCallExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irErrorCallExpression(builder: IrErrorCallExpressionBuilder.() -> Unit): IrErrorCallExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(errorCallExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.errorCallExpressionBuilder(): IrErrorCallExpressionBuilder =
    IrErrorCallExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
