/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrDoWhileLoopImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
) : IrDoWhileLoop() {
    override var label: String? = null
    override var body: IrExpression? = null
    override lateinit var condition: IrExpression

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}

fun IrDoWhileLoopImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
) = IrDoWhileLoopImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)
