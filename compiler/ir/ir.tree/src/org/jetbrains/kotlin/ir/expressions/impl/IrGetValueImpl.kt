/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrGetValueImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override val symbol: IrValueSymbol,
    override val origin: IrStatementOrigin? = null
) : IrGetValue() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        symbol: IrValueSymbol,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, symbol.owner.type, symbol, origin)
}

fun IrGetValue.copyWithOffsets(newStartOffset: Int, newEndOffset: Int): IrGetValue =
    IrGetValueImpl(newStartOffset, newEndOffset, type, symbol, origin)
