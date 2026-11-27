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
import org.jetbrains.kotlin.ir.expressions.IrDynamicMemberExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicMemberExpressionImpl

class IrDynamicMemberExpressionBuilder @PublishedApi internal constructor() : IrDynamicExpressionBuilder() {
    lateinit var memberName: String

    lateinit var receiver: IrExpression
}

@PublishedApi
internal fun build(builder: IrDynamicMemberExpressionBuilder): IrDynamicMemberExpression {
    val node = IrDynamicMemberExpressionImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        memberName = builder.memberName,
        receiver = builder.receiver,
    )
    return node
}

inline fun buildDynamicMemberExpression(builder: IrDynamicMemberExpressionBuilder.() -> Unit): IrDynamicMemberExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrDynamicMemberExpressionBuilder().apply(builder))
}

inline fun IrBuilder.irDynamicMemberExpression(builder: IrDynamicMemberExpressionBuilder.() -> Unit): IrDynamicMemberExpression {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(dynamicMemberExpressionBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.dynamicMemberExpressionBuilder(): IrDynamicMemberExpressionBuilder =
    IrDynamicMemberExpressionBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
