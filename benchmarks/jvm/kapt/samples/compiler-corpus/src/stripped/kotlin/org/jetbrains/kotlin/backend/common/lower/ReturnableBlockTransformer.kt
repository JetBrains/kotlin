/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.isSubtypeOf

// TODO migrate other usages and move this file to backend.jvm
/**
 * Replaces returnable blocks and `return`'s with loops and `break`'s correspondingly.
 *
 * Converts returnable blocks into regular composite blocks when the only `return` is the last statement.
 *
 *     block {
 *       ...
 *       return@block e
 *       ...
 *     }
 *
 * is transformed into
 *
 *     {
 *       val result
 *       loop@ do {
 *         ...
 *         {
 *           result = e
 *           break@loop
 *         }
 *         ...
 *       } while (false)
 *       result
 *     }
 *
 * When the only `return` for the block is the last statement:
 *
 *     block {
 *       ...
 *       return@block e
 *     }
 *
 * is transformed into
 *
 *     {
 *       ...
 *       e
 *     }
 */
class ReturnableBlockTransformer(
    val context: CommonBackendContext,
    val containerSymbol: IrSymbol? = null,
) : IrElementTransformerVoidWithContext() {
    private var labelCnt = 0
    private val returnMap = mutableMapOf<IrReturnableBlockSymbol, (IrReturn) -> IrExpression>()

    override fun visitReturn(expression: IrReturn): IrExpression { TODO() }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression { TODO() }
}
