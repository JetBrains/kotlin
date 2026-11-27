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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.expressions.impl.IrSuspensionPointImpl

class IrSuspensionPointBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    lateinit var suspensionPointIdParameter: IrVariable

    lateinit var result: IrExpression

    lateinit var resumeResult: IrExpression
}

@PublishedApi
internal fun build(builder: IrSuspensionPointBuilder): IrSuspensionPoint {
    val node = IrSuspensionPointImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        suspensionPointIdParameter = builder.suspensionPointIdParameter,
        result = builder.result,
        resumeResult = builder.resumeResult,
    )
    return node
}

inline fun buildSuspensionPoint(builder: IrSuspensionPointBuilder.() -> Unit): IrSuspensionPoint {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSuspensionPointBuilder().apply(builder))
}

inline fun IrBuilder.irSuspensionPoint(builder: IrSuspensionPointBuilder.() -> Unit): IrSuspensionPoint {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(suspensionPointBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.suspensionPointBuilder(): IrSuspensionPointBuilder =
    IrSuspensionPointBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
