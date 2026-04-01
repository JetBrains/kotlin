/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.utils.filterOutSymbolsFromCache
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.ir.symbols.*

class Fir2IrScopeCache() {
    val parameters: Map<FirValueParameter, IrValueParameterSymbol>
        field = mutableMapOf<FirValueParameter, IrValueParameterSymbol>()

    val variables: Map<FirVariable, IrVariableSymbol>
        field = mutableMapOf<FirVariable, IrVariableSymbol>()

    val localFunctions: Map<FirFunction, IrSimpleFunctionSymbol>
        field = mutableMapOf<FirFunction, IrSimpleFunctionSymbol>()

    val delegatedProperties: Map<FirProperty, IrLocalDelegatedPropertySymbol>
        field = mutableMapOf<FirProperty, IrLocalDelegatedPropertySymbol>()

    constructor(
        initParameters: Map<FirValueParameter, IrValueParameterSymbol>,
        initVariables: Map<FirVariable, IrVariableSymbol>,
        initLocalFunctions: Map<FirFunction, IrSimpleFunctionSymbol>,
        initDelegatedProperties: Map<FirProperty, IrLocalDelegatedPropertySymbol>,
    ) : this() {
        parameters.putAll(initParameters)
        variables.putAll(initVariables)
        localFunctions.putAll(initLocalFunctions)
        delegatedProperties.putAll(initDelegatedProperties)
    }

    fun getParameter(parameter: FirValueParameter): IrValueParameterSymbol? {
        return parameters[parameter]
    }

    fun putParameter(firParameter: FirValueParameter, irParameterSymbol: IrValueParameterSymbol) {
        parameters[firParameter] = irParameterSymbol
    }

    fun getVariable(variable: FirVariable): IrVariableSymbol? {
        return variables[variable]
    }

    fun putVariable(firVariable: FirVariable, irVariableSymbol: IrVariableSymbol) {
        variables[firVariable] = irVariableSymbol
    }

    fun getLocalFunction(localFunction: FirFunction): IrSimpleFunctionSymbol? {
        return localFunctions[localFunction]
    }

    fun putLocalFunction(localFunction: FirFunction, irFunctionSymbol: IrSimpleFunctionSymbol) {
        require(localFunction !is FirNamedFunction || localFunction.visibility == Visibilities.Local) {
            "Function is not local: ${localFunction.render()}"
        }
        localFunctions[localFunction] = irFunctionSymbol
    }

    fun getDelegatedProperty(property: FirProperty): IrLocalDelegatedPropertySymbol? {
        return delegatedProperties[property]
    }

    fun putDelegatedProperty(firProperty: FirProperty, irPropertySymbol: IrLocalDelegatedPropertySymbol) {
        delegatedProperties[firProperty] = irPropertySymbol
    }

    fun isEmpty(): Boolean {
        return parameters.isEmpty()
                && variables.isEmpty()
                && localFunctions.isEmpty()
                && delegatedProperties.isEmpty()
    }

    // Should be updated respectively when adding new properties
    fun cloneFilteringSymbols(filterOutSymbols: Set<FirBasedSymbol<*>>): Fir2IrScopeCache {
        return Fir2IrScopeCache(
            filterOutSymbolsFromCache(parameters, filterOutSymbols),
            filterOutSymbolsFromCache(variables, filterOutSymbols),
            filterOutSymbolsFromCache(localFunctions, filterOutSymbols),
            filterOutSymbolsFromCache(delegatedProperties, filterOutSymbols)
        )
    }

    fun clear() {
        parameters.clear()
        variables.clear()
        localFunctions.clear()
        delegatedProperties.clear()
    }
}
