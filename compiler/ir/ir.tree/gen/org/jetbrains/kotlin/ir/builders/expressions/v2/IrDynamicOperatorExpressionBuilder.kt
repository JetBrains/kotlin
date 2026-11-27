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
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl

class IrDynamicOperatorExpressionBuilder @PublishedApi internal constructor() : IrDynamicExpressionBuilder() {
    lateinit var operator: IrDynamicOperator

    lateinit var receiver: IrExpression

    val arguments: MutableList<IrExpression> = []
}

@PublishedApi
internal fun build(builder: IrDynamicOperatorExpressionBuilder): IrDynamicOperatorExpression {
    val node = IrDynamicOperatorExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        operator = builder.operator,
    )
    node.receiver = builder.receiver
    node.arguments.addAll(builder.arguments)
    return node
}

inline fun buildDynamicOperatorExpression(builder: IrDynamicOperatorExpressionBuilder.() -> Unit): IrDynamicOperatorExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrDynamicOperatorExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irDynamicOperatorExpression(builder: IrDynamicOperatorExpressionBuilder.() -> Unit): IrDynamicOperatorExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(dynamicOperatorExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.dynamicOperatorExpressionBuilder(): IrDynamicOperatorExpressionBuilder =
    IrDynamicOperatorExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
