/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

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
class ReturnableBlockLowering(val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(ReturnableBlockTransformer(context), ReturnableBlockLoweringContext(irFile))
    }
}

private class ReturnableBlockLoweringContext(val containingDeclaration: IrSymbolOwner) {
    var labelCnt = 0
    val returnMap = mutableMapOf<IrReturnableBlockSymbol, (IrReturn) -> IrExpression>()
}

private class ReturnableBlockTransformer(
    val context: JsIrBackendContext
) : IrElementTransformer<ReturnableBlockLoweringContext> {

    override fun visitReturn(expression: IrReturn, data: ReturnableBlockLoweringContext): IrExpression {
        expression.transformChildren(this, data)
        return data.returnMap[expression.returnTargetSymbol]?.invoke(expression) ?: expression
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: ReturnableBlockLoweringContext): IrStatement {
        if (declaration is IrSymbolOwner) {
            declaration.transformChildren(this, ReturnableBlockLoweringContext(declaration))
        }
        return super.visitDeclaration(declaration, data)
    }

    private val constFalse = JsIrBuilder.buildBoolean(context.builtIns.booleanType, false)

    override fun visitContainerExpression(expression: IrContainerExpression, data: ReturnableBlockLoweringContext): IrExpression {
        if (expression !is IrReturnableBlock) return super.visitContainerExpression(expression, data)

        val variable by lazy {
            JsSymbolBuilder.buildTempVar(
                data.containingDeclaration.symbol,
                expression.type,
                "tmp\$ret\$${data.labelCnt++}",
                true
            )
        }

        val loop by lazy {
            IrDoWhileLoopImpl(
                expression.startOffset,
                expression.endOffset,
                context.builtIns.unitType,
                expression.origin
            ).apply {
                label = "l\$ret\$${data.labelCnt++}"
                condition = constFalse
            }
        }

        var hasReturned = false

        data.returnMap[expression.symbol] = { returnExpression ->
            hasReturned = true

            IrCompositeImpl(
                returnExpression.startOffset,
                returnExpression.endOffset,
                context.builtIns.unitType
            ).apply {
                statements += JsIrBuilder.buildSetVariable(variable, returnExpression.value)
                statements += JsIrBuilder.buildBreak(context.builtIns.unitType, loop)
            }
        }

        val newStatements = expression.statements.mapIndexed { i, s ->
            if (i == expression.statements.lastIndex && s is IrReturn && s.returnTargetSymbol == expression.symbol) {
                s.transformChildren(this, data)
                if (!hasReturned) s.value else {
                    JsIrBuilder.buildSetVariable(variable, s.value)
                }
            } else {
                s.transform(this, data)
            }
        }

        data.returnMap.remove(expression.symbol)

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
                context.builtIns.unitType,
                expression.origin,
                newStatements
            )

            return IrCompositeImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                expression.origin
            ).apply {
                statements += JsIrBuilder.buildVar(variable)
                statements += loop
                statements += JsIrBuilder.buildGetValue(variable)
            }
        }
    }
}