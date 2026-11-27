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
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl

open class IrBranchBuilder @PublishedApi internal constructor() : IrElementBuilder() {
    lateinit var condition: IrExpression

    lateinit var result: IrExpression
}

@PublishedApi
internal fun build(builder: IrBranchBuilder): IrBranch {
    val node = IrBranchImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        condition = builder.condition,
        result = builder.result,
    )
    return node
}

inline fun buildBranch(builder: IrBranchBuilder.() -> Unit): IrBranch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrBranchBuilder().apply(builder))
}

inline fun IrBuilder.irBranch(builder: IrBranchBuilder.() -> Unit): IrBranch {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(branchBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilder.branchBuilder(): IrBranchBuilder =
    IrBranchBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
