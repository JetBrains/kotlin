/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.backend.common.lower.copyAndActualizeDefaultValue
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class FunctionDefaultParametersActualizer(private val expectActualMap: Map<IrSymbol, IrSymbol>) {
    fun actualize() {
        for ((expect, actual) in expectActualMap) {
            if (expect is IrFunctionSymbol) {
                actualize(expect.owner, (actual as IrFunctionSymbol).owner)
            }
        }
    }

    private fun actualize(expectFunction: IrFunction, actualFunction: IrFunction) {
        expectFunction.valueParameters.zip(actualFunction.valueParameters).forEach { (expectParameter, actualParameter) ->
            val expectDefaultValue = expectParameter.defaultValue
            if (actualParameter.defaultValue == null && expectDefaultValue != null) {
                actualParameter.defaultValue = expectDefaultValue.copyAndActualizeDefaultValue(
                    actualFunction,
                    actualParameter,
                    expectFunction.mapTypeParametersTo(actualFunction),
                    classActualizer = { (expectActualMap[it.symbol] as? IrClassSymbol)?.owner },
                    functionActualizer = { (expectActualMap[it.symbol] as? IrFunctionSymbol)?.owner },
                )
            }
        }
    }

    private fun IrDeclaration.mapTypeParametersTo(actual: IrDeclaration): Map<IrTypeParameter, IrTypeParameter> {
        val typeParameters = mutableMapOf<IrTypeParameter, IrTypeParameter>()
        var currentActual: IrDeclaration? = actual
        var currentExpect: IrDeclaration? = this

        while (currentExpect != null && currentActual != null) {
            if (currentExpect is IrTypeParametersContainer && currentActual is IrTypeParametersContainer) {
                typeParameters += currentExpect.typeParameters.zip(currentActual.typeParameters).toMap()
            }

            currentExpect = currentExpect.parent as? IrDeclaration
            currentActual = currentActual.parent as? IrDeclaration
        }

        return typeParameters
    }
}