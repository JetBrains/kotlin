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
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

// Gets rid of IrReturnableBlock
// Returnable block -> loop
class ReturnableBlockLowering(val context: JsIrBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transform(visitor, null)
    }

    private var containingDeclaration: IrSymbolOwner? = null

    private var labelCnt = 0
    private val constFalse = JsIrBuilder.buildBoolean(context.builtIns.booleanType, false)


    private var tmpVarCounter = 0;

    class ReturnInfo(
        val resultVariable: IrVariableSymbol,
        val loop: IrLoop
    ) {
        var cnt = 0
    }

    private val returnMap = mutableMapOf<IrReturnableBlockSymbol, ReturnInfo>()

    val visitor = object : IrElementTransformerVoid() {

        private fun IrReturn.patchReturnTo(info: ReturnInfo): IrExpression {
            info.cnt++

            val compoundBlock = IrCompositeImpl(
                startOffset,
                endOffset,
                context.builtIns.unitType
            )

            compoundBlock.statements += JsIrBuilder.buildSetVariable(info.resultVariable, value)
            compoundBlock.statements += JsIrBuilder.buildBreak(context.builtIns.unitType, info.loop)

            return compoundBlock
        }

        override fun visitReturn(expression: IrReturn): IrExpression {
            expression.transformChildren(this, null)
            return returnMap[expression.returnTargetSymbol]?.let { info ->
                expression.patchReturnTo(info)
            } ?: expression
        }



        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
            if (declaration is IrSymbolOwner) {
                containingDeclaration = declaration
            }
            return super.visitDeclaration(declaration)
        }

        override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
            if (expression is IrReturnableBlock) {

                val replacementBlock = IrCompositeImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.origin
                )

                val variable = JsSymbolBuilder.buildTempVar(containingDeclaration!!.symbol, expression.type, "tmp\$ret\$${tmpVarCounter++}", true)
                val varDeclaration = JsIrBuilder.buildVar(variable)
                replacementBlock.statements += varDeclaration

                val block = IrCompositeImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType,
                    expression.origin
                )

                val loop = IrDoWhileLoopImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType,
                    expression.origin
                ).apply {
                    label = "l_${labelCnt++}"
                    condition = constFalse
                    body = block
                }

                val returnInfo = ReturnInfo(variable, loop)
                returnMap[expression.symbol] = returnInfo


                expression.statements.let { list ->
                    for (i in list.indices) {
                        val s = list[i]
                        list[i] =
                                if (i == list.lastIndex && returnInfo.cnt == 0 && s is IrReturn && s.returnTargetSymbol == expression.symbol) {
                                    s.transformChildren(this, null)
                                    if (returnInfo.cnt == 0) s.value else {
                                        JsIrBuilder.buildSetVariable(variable, s.value)
                                    }
                                } else {
                                    s.transform(this, null)
                                }
                    }
                }

                block.statements += expression.statements

                if (returnInfo.cnt == 0) {
                    return block
                }

                replacementBlock.statements += loop
                replacementBlock.statements += JsIrBuilder.buildGetValue(variable)

                return replacementBlock

            } else return super.visitContainerExpression(expression)
        }
    }
}