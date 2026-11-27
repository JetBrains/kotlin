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
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl

class IrStringConcatenationBuilder @PublishedApi internal constructor() : IrExpressionBuilder() {
    val arguments: MutableList<IrExpression> = []
}

@PublishedApi
internal fun build(builder: IrStringConcatenationBuilder): IrStringConcatenation {
    val node = IrStringConcatenationImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
    )
    node.arguments.addAll(builder.arguments)
    return node
}

inline fun buildStringConcatenation(builder: IrStringConcatenationBuilder.() -> Unit): IrStringConcatenation {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrStringConcatenationBuilder().apply(builder))
}

inline fun IrBuilder.irStringConcatenation(builder: IrStringConcatenationBuilder.() -> Unit = {}): IrStringConcatenation {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(stringConcatenationBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.stringConcatenationBuilder(): IrStringConcatenationBuilder =
    IrStringConcatenationBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.type = context.irBuiltIns.stringType
    }
