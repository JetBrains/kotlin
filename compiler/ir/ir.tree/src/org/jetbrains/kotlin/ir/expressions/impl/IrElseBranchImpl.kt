/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression

class IrElseBranchImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var condition: IrExpression,
    override var result: IrExpression
) : IrElseBranch() {
    constructor(condition: IrExpression, result: IrExpression) :
            this(condition.startOffset, result.endOffset, condition, result)
}