/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.ir.symbols.*

class Fir2IrScopeCache {
    private val parameterCache = mutableMapOf<FirValueParameter, IrValueParameterSymbol>()

    private val variableCache = mutableMapOf<FirVariable, IrVariableSymbol>()

    private val localFunctionCache = mutableMapOf<FirFunction, IrSimpleFunctionSymbol>()

    val localFunctions: Map<FirFunction, IrSimpleFunctionSymbol>
        get() = localFunctionCache

    private val delegatedPropertyCache = mutableMapOf<FirProperty, IrLocalDelegatedPropertySymbol>()

    fun getParameter(parameter: FirValueParameter): IrValueParameterSymbol? {
        return parameterCache[parameter]
    }

    fun putParameter(firParameter: FirValueParameter, irParameterSymbol: IrValueParameterSymbol) {
        parameterCache[firParameter] = irParameterSymbol
    }

    fun getVariable(variable: FirVariable): IrVariableSymbol? {
        return variableCache[variable]
    }

    fun putVariable(firVariable: FirVariable, irVariableSymbol: IrVariableSymbol) {
        variableCache[firVariable] = irVariableSymbol
    }

    fun getLocalFunction(localFunction: FirFunction): IrSimpleFunctionSymbol? {
        return localFunctionCache[localFunction]
    }

    fun putLocalFunction(localFunction: FirFunction, irFunctionSymbol: IrSimpleFunctionSymbol) {
        require(localFunction !is FirSimpleFunction || localFunction.visibility == Visibilities.Local)
        localFunctionCache[localFunction] = irFunctionSymbol
    }

    fun getDelegatedProperty(property: FirProperty): IrLocalDelegatedPropertySymbol? {
        return delegatedPropertyCache[property]
    }

    fun putDelegatedProperty(firProperty: FirProperty, irPropertySymbol: IrLocalDelegatedPropertySymbol) {
        delegatedPropertyCache[firProperty] = irPropertySymbol
    }

    fun clear() {
        parameterCache.clear()
        variableCache.clear()
        localFunctionCache.clear()
        delegatedPropertyCache.clear()
    }
}
