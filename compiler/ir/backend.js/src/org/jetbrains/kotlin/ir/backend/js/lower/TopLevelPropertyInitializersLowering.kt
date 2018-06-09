/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.initialize
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.transformFlat

object TopLevelPropertyInitializersLowering : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.declarations.transformFlat {
            if (it is IrField) {
                it.initializer?.apply {
                    if (expression is IrStatementContainer) {
                        val initFnSymbol = JsSymbolBuilder.buildSimpleFunction(irFile.symbol.descriptor, it.name.asString() + "\$init\$")
                            .initialize(type = expression.type)

                        val initFn = JsIrBuilder.buildFunction(initFnSymbol).apply {
                            body = IrBlockBodyImpl(expression.startOffset, expression.endOffset).apply {
                                statements += JsIrBuilder.buildReturn(initFnSymbol, expression)
                            }
                        }

                        expression = JsIrBuilder.buildCall(initFnSymbol)

                        return@transformFlat listOf(initFn, it)
                    }
                }
            }
            listOf(it)
        }
    }

}