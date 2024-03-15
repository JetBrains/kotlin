/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrCompositeImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
) : IrComposite() {
    override val statements: MutableList<IrStatement> = ArrayList(2)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

fun IrCompositeImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
) = IrCompositeImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrCompositeImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    statements: List<IrStatement>,
) = IrCompositeImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
).apply {
    this.statements.addAll(statements)
}
