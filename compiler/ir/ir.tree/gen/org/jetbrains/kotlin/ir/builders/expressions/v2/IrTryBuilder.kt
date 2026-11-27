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
import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl

class IrTryBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var tryResult: IrExpression

    val catches: MutableList<IrCatch> = []

    var finallyExpression: IrExpression? = null
}

@PublishedApi
internal fun build(builder: IrTryBuilder): IrTry {
    val node = IrTryImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
    )
    node.tryResult = builder.tryResult
    node.catches.addAll(builder.catches)
    node.finallyExpression = builder.finallyExpression
    return node
}

inline fun buildTry(builder: IrTryBuilder.() -> Unit): IrTry {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrTryBuilder().apply(builder))
}

inline fun IrBuilder.irTry(builder: IrTryBuilder.() -> Unit): IrTry {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(tryBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.tryBuilder(): IrTryBuilder =
    IrTryBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
