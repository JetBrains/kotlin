/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType

class IrInlinedFunctionBlockImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var inlineCall: IrFunctionAccessExpression,
    override var inlinedElement: IrElement,
    override var origin: IrStatementOrigin? = null,
) : IrInlinedFunctionBlock() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        inlineCall: IrFunctionAccessExpression,
        inlinedElement: IrElement,
        origin: IrStatementOrigin?,
        statements: List<IrStatement>,
    ) : this(startOffset, endOffset, type, inlineCall, inlinedElement, origin) {
        this.statements.addAll(statements)
    }
}