/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.descriptors.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.transform
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

// Gets rid of IrReturnableBlock
// Returnable block -> loop
class ReturnableBlockLowering(val context: JsIrBackendContext) : DeclarationContainerLoweringPass {
    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transform { memberDeclaration ->
            if (memberDeclaration is IrFunction) {
                memberDeclaration.lower()
            } else memberDeclaration
        }
    }

    fun IrFunction.lower(): IrFunction {
        function = this
        body?.accept(visitor, null)
        return this
    }

    private var function: IrFunction? = null

    private var labelCnt = 0
    private val constFalse = JsIrBuilder.buildBoolean(context.builtIns.booleanType, false)


    private var tmpVarCounter = 0;


    data class ReturnInfo(
        val resultVariable: IrVariableSymbol,
        val loop: IrLoop
    )

    private val returnMap = mutableMapOf<IrReturnableBlockSymbol, ReturnInfo>()

    private val unitValue =
        JsIrBuilder.buildGetObjectValue(context.builtIns.unitType, context.symbolTable.referenceClass(context.builtIns.unit))

    val visitor = object : IrElementTransformerVoid() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            expression.transformChildren(this, null)
            return returnMap[expression.returnTargetSymbol]?.let { (v, loop) ->
                // TODO IrComposite maybe?
                val compoundBlock = IrBlockImpl(
                    expression.startOffset,
                    expression.endOffset,
                    context.builtIns.unitType
                )

                compoundBlock.statements += JsIrBuilder.buildSetVariable(v, expression.value)
                compoundBlock.statements += JsIrBuilder.buildBreak(context.builtIns.unitType, loop)

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

                val variable = JsSymbolBuilder.buildTempVar(function!!.symbol, expression.type, "tmp\$slfk\$${tmpVarCounter++}", true)
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

                returnMap[expression.symbol] = ReturnInfo(variable, loop)

                expression.transformChildren(this, null)

                block.statements += expression.statements

                block.statements.lastOrNull()?.let {
                    if (it is IrExpression) {
                        if (it.type == context.builtIns.unitType) {
                            block.statements += JsIrBuilder.buildSetVariable(variable, unitValue)
                        } else {
                            block.statements[block.statements.size - 1] = JsIrBuilder.buildSetVariable(variable, it)
                        }
                    }
                }

                replacementBlock.statements += loop
                replacementBlock.statements += JsIrBuilder.buildGetValue(variable)

                return replacementBlock

            } else return super.visitContainerExpression(expression)
        }
    }
}