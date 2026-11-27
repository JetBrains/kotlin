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
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrSuspendableExpressionImpl

class IrSuspendableExpressionBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var suspensionPointId: IrExpression

    lateinit var result: IrExpression
}

@PublishedApi
internal fun build(builder: IrSuspendableExpressionBuilder): IrSuspendableExpression {
    val node = IrSuspendableExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        suspensionPointId = builder.suspensionPointId,
        result = builder.result,
    )
    return node
}

inline fun buildSuspendableExpression(builder: IrSuspendableExpressionBuilder.() -> Unit): IrSuspendableExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSuspendableExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irSuspendableExpression(builder: IrSuspendableExpressionBuilder.() -> Unit): IrSuspendableExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(suspendableExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.suspendableExpressionBuilder(): IrSuspendableExpressionBuilder =
    IrSuspendableExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
