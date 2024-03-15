/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrBlockImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
) : IrBlock() {
    override val statements: MutableList<IrStatement> = ArrayList(2)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

fun IrBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
) = IrBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
    statements: List<IrStatement>,
) = IrBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
).apply {
    this.statements.addAll(statements)
}

fun IrBlockImpl.addIfNotNull(statement: IrStatement?) {
    if (statement != null) statements.add(statement)
}

fun IrBlockImpl.inlineStatement(statement: IrStatement) {
    if (statement is IrBlock) {
        statements.addAll(statement.statements)
    } else {
        statements.add(statement)
    }
}
