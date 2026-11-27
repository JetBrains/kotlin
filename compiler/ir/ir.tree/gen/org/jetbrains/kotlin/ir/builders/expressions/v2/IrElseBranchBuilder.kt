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
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl

class IrElseBranchBuilder @PublishedApi internal constructor() : IrBranchBuilder()

@PublishedApi
internal fun build(builder: IrElseBranchBuilder): IrElseBranch {
    val node = IrElseBranchImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        condition = builder.condition,
        result = builder.result,
    )
    return node
}

inline fun buildElseBranch(builder: IrElseBranchBuilder.() -> Unit): IrElseBranch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrElseBranchBuilder().apply(builder))
}

inline fun IrBuilder.irElseBranch(builder: IrElseBranchBuilder.() -> Unit): IrElseBranch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(elseBranchBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.elseBranchBuilder(): IrElseBranchBuilder =
    IrElseBranchBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
        it.condition = IrConstImpl.boolean(startOffset, endOffset, context.irBuiltIns.booleanType, true)
    }
