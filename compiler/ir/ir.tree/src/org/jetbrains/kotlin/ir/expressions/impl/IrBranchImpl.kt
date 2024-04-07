/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

open class IrBranchImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var condition: IrExpression,
    override var result: IrExpression,
) : IrBranch()

fun IrBranchImpl(
    startOffset: Int,
    endOffset: Int,
    condition: IrExpression,
    result: IrExpression,
) = IrBranchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    condition = condition,
    result = result,
)

fun IrBranchImpl(
    condition: IrExpression,
    result: IrExpression,
) = IrBranchImpl(
    constructorIndicator = null,
    startOffset = condition.startOffset,
    endOffset = result.endOffset,
    condition = condition,
    result = result,
)
