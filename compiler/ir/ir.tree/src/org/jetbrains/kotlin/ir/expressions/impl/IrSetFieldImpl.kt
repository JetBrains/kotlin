/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IrElementConstructorIndicator

class IrSetFieldImpl internal constructor(
    @Suppress("UNUSED_PARAMETER")
    constructorIndicator: IrElementConstructorIndicator?,
    override val startOffset: Int,
    override val endOffset: Int,
    override var symbol: IrFieldSymbol,
    override var type: IrType,
    override var origin: IrStatementOrigin?,
    override var superQualifierSymbol: IrClassSymbol?,
) : IrSetField() {
    override var receiver: IrExpression? = null

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    override lateinit var value: IrExpression
}

fun IrSetFieldImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrFieldSymbol,
    type: IrType,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrSetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
)

fun IrSetFieldImpl(
    startOffset: Int, endOffset: Int,
    symbol: IrFieldSymbol,
    receiver: IrExpression?,
    value: IrExpression,
    type: IrType,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrSetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
).apply {
    this.receiver = receiver
    this.value = value
}