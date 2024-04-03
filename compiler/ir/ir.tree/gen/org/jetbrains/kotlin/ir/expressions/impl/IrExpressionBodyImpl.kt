/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

class IrExpressionBodyImpl @IrImplementationDetail internal constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override var expression: IrExpression,
) : IrExpressionBody()
