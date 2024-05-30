/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.remapSymbolParent

internal class FunctionDefaultParametersActualizer(
    symbolRemapper: ActualizerSymbolRemapper,
    private val expectActualMap: IrExpectActualMap
) {
    private val visitor = FunctionDefaultParametersActualizerVisitor(symbolRemapper)

    fun actualize() {
        for ((expect, actual) in expectActualMap.regularSymbols) {
            if (expect is IrFunctionSymbol) {
                actualize(expect.owner, (actual as IrFunctionSymbol).owner)
            }
        }
    }

    private fun actualize(expectFunction: IrFunction, actualFunction: IrFunction) {
        expectFunction.valueParameters.zip(actualFunction.valueParameters).forEach { (expectParameter, actualParameter) ->
            val expectDefaultValue = expectParameter.defaultValue
            if (actualParameter.defaultValue == null && expectDefaultValue != null) {
                actualParameter.defaultValue = expectDefaultValue.deepCopyWithSymbols(actualFunction).transform(visitor, null)
            }
        }
    }
}

private class FunctionDefaultParametersActualizerVisitor(private val symbolRemapper: SymbolRemapper) : ActualizerVisitor(symbolRemapper) {
    override fun visitGetValue(expression: IrGetValue): IrGetValue {
        // It performs actualization of dispatch/extension receivers
        // It's actual only for default parameter values of expect functions because expect functions don't have bodies
        return expression.remapSymbolParent(
            classRemapper = { symbolRemapper.getReferencedClass(it.symbol).owner },
            functionRemapper = { symbolRemapper.getReferencedFunction(it.symbol).owner }
        ).copyAttributes(expression)
    }
}
