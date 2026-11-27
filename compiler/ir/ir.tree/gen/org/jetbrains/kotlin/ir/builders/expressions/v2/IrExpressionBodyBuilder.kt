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
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

class IrExpressionBodyBuilder @PublishedApi internal constructor() : IrBodyBuilder() {
    lateinit var expression: IrExpression
}

@PublishedApi
internal fun IrFactory.build(builder: IrExpressionBodyBuilder): IrExpressionBody {
    val node = createExpressionBody(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        expression = builder.expression,
    )
    return node
}

inline fun IrFactory.irExpressionBody(builder: IrExpressionBodyBuilder.() -> Unit): IrExpressionBody {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrExpressionBodyBuilder().apply(builder))
}
