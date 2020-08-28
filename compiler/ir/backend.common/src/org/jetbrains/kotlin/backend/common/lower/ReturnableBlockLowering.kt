/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.transformStatement

/**
 * Replaces returnable blocks and `return`'s with loops and `break`'s correspondingly.
 *
 * Converts returnable blocks into regular composite blocks when the only `return` is the last statement.
 *
 * ```
 * block {
 *   ...
 *   return@block e
 *   ...
 * }
 * ```
 *
 * is transformed into
 *
 * ```
 * {
 *   val result
 *   loop@ do {
 *     ...
 *     {
 *       result = e
 *       break@loop
 *     }
 *     ...
 *   } while (false)
 *   result
 * }
 * ```
 *
 * When the only `return` for the block is the last statement:
 *
 * ```
 * block {
 *   ...
 *   return@block e
 * }
 * ```
 *
 * is transformed into
 *
 * {
 *   ...
 *   e
 * }
 *
 */
class ReturnableBlockLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        container.transform(ReturnableBlockTransformer(context, (container as IrSymbolOwner).symbol), null)
    }
}

class ReturnableBlockTransformer(val context: CommonBackendContext, val containerSymbol: IrSymbol? = null) : IrElementTransformerVoidWithContext() {
    private var labelCnt = 0
    private val returnMap = mutableMapOf<IrReturnableBlockSymbol, (IrReturn) -> IrExpression>()

    override fun visitReturn(expression: IrReturn): IrExpression {
        expression.transformChildrenVoid()
        return returnMap[expression.returnTargetSymbol]?.invoke(expression) ?: expression
    }

    override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
        if (expression !is IrReturnableBlock) return super.visitContainerExpression(expression)

        val scopeSymbol = currentScope?.scope?.scopeOwnerSymbol ?: containerSymbol
        val builder = context.createIrBuilder(scopeSymbol!!)
        val variable by lazy {
            builder.scope.createTmpVariable(expression.type, "tmp\$ret\$${labelCnt++}", true)
        }

        val loop by lazy {
            IrDoWhileLoopImpl(
                expression.startOffset,
                expression.endOffset,
                context.irBuiltIns.unitType,
                expression.origin
            ).apply {
                label = "l\$ret\$${labelCnt++}"
                condition = builder.irBoolean(false)
            }
        }

        var hasReturned = false

        returnMap[expression.symbol] = { returnExpression ->
            hasReturned = true
            builder.irComposite(returnExpression) {
                +irSetVar(variable.symbol, returnExpression.value)
                +irBreak(loop)
            }
        }

        val newStatements = expression.statements.mapIndexed { i, s ->
            if (i == expression.statements.lastIndex && s is IrReturn && s.returnTargetSymbol == expression.symbol) {
                s.transformChildrenVoid()
                if (!hasReturned) s.value else {
                    builder.irSetVar(variable.symbol, s.value)
                }
            } else {
                s.transformStatement(this)
            }
        }

        returnMap.remove(expression.symbol)

        if (!hasReturned) {
            return IrCompositeImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.origin,
                newStatements
            )
        } else {
            loop.body = IrBlockImpl(
                expression.startOffset,
                expression.endOffset,
                context.irBuiltIns.unitType,
                expression.origin,
                newStatements
            )

            return builder.irComposite(expression, expression.origin) {
                +variable
                +loop
                +irGet(variable)
            }
        }
    }
}