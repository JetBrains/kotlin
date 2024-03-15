/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrElseBranchImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var condition: IrExpression,
    override var result: IrExpression,
) : IrElseBranch()

fun IrElseBranchImpl(
    startOffset: Int,
    endOffset: Int,
    condition: IrExpression,
    result: IrExpression,
) = IrElseBranchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    condition = condition,
    result = result,
)

fun IrElseBranchImpl(
    condition: IrExpression,
    result: IrExpression,
) = IrElseBranchImpl(
    constructorIndicator = null,
    startOffset = condition.startOffset,
    endOffset = result.endOffset,
    condition = condition,
    result = result,
)
