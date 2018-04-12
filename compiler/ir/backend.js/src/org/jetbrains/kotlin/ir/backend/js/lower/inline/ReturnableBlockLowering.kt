/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

// Gets rid of IrReturnableBlock
// Returnable block -> loop
class ReturnableBlockLowering(val context: JsIrBackendContext) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transform { memberDeclaration ->
            when (memberDeclaration) {
                is IrFunction -> memberDeclaration.lower()
                is IrProperty -> memberDeclaration.lower()
                else -> memberDeclaration
            }
        }
    }

    fun IrFunction.lower(): IrFunction {
        containingDeclaration = this
        body?.accept(visitor, null)
        return this
    }

    fun IrProperty.lower(): IrProperty {
        backingField?.let {
            containingDeclaration = it
            it.initializer?.accept(visitor, null)
        }
        return this
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
        override fun visitReturn(expression: IrReturn): IrExpression {
            expression.transformChildren(this, null)
            return returnMap[expression.returnTargetSymbol]?.let { info ->
                info.cnt++

                // TODO IrComposite maybe?
                val compoundBlock = IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType
                )

                compoundBlock.statements += JsIrBuilder.buildSetVariable(info.resultVariable, expression.value)
                compoundBlock.statements += JsIrBuilder.buildBreak(context.builtIns.unitType, info.loop)

                compoundBlock
            } ?: expression
        }

        override fun visitContainerExpression(expression: IrContainerExpression): IrExpression {
            if (expression is IrReturnableBlock) {

                val replacementBlock = IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    expression.type,
                    expression.origin
                )

                val variable = JsSymbolBuilder.buildTempVar(containingDeclaration!!.symbol, expression.type, "tmp\$slfk\$${tmpVarCounter++}", true)
                val varDeclaration = JsIrBuilder.buildVar(variable)
                replacementBlock.statements += varDeclaration

                val block = IrBlockImpl(
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
                                    s.value.transform(this, null)
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