/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolDescriptor
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

internal class DelegatedLocalPropertyLValue(
    private val context: IrGeneratorContext,
    val startOffset: Int,
    val endOffset: Int,
    override val type: IrType,
    private val getterSymbol: IrSimpleFunctionSymbol?,
    private val setterSymbol: IrSimpleFunctionSymbol?,
    val origin: IrStatementOrigin? = null
) :
    LValue,
    AssignmentReceiver {

    override fun load(): IrExpression =
        IrCallImpl.fromSymbolDescriptor(startOffset, endOffset, type, getterSymbol!!, origin = origin)

    override fun store(irExpression: IrExpression): IrExpression =
            IrCallImpl.fromSymbolDescriptor(startOffset, endOffset, context.irBuiltIns.unitType, setterSymbol!!, origin = origin).apply {
            putValueArgument(0, irExpression)
        }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        withLValue(this)
}
