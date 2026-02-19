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
    private val parameterCache = mutableMapOf<FirValueParameter, IrValueParameterSymbol>()

    val parameters: Map<FirValueParameter, IrValueParameterSymbol>
        get() = parameterCache

    private val variableCache = mutableMapOf<FirVariable, IrVariableSymbol>()

    val variables: Map<FirVariable, IrVariableSymbol>
        get() = variableCache

    private val localFunctionCache = mutableMapOf<FirFunction, IrSimpleFunctionSymbol>()

    val localFunctions: Map<FirFunction, IrSimpleFunctionSymbol>
        get() = localFunctionCache

    private val delegatedPropertyCache = mutableMapOf<FirProperty, IrLocalDelegatedPropertySymbol>()

    val delegatedProperties: Map<FirProperty, IrLocalDelegatedPropertySymbol>
        get() = delegatedPropertyCache

    constructor(
        parameters: Map<FirValueParameter, IrValueParameterSymbol>,
        variables: Map<FirVariable, IrVariableSymbol>,
        localFunctions: Map<FirFunction, IrSimpleFunctionSymbol>,
        delegatedProperties: Map<FirProperty, IrLocalDelegatedPropertySymbol>,
    ) : this() {
        parameterCache.putAll(parameters)
        variableCache.putAll(variables)
        localFunctionCache.putAll(localFunctions)
        delegatedPropertyCache.putAll(delegatedProperties)
    }

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
        require(localFunction !is FirNamedFunction || localFunction.visibility == Visibilities.Local) {
            "Function is not local: ${localFunction.render()}"
        }
        localFunctionCache[localFunction] = irFunctionSymbol
    }

    fun getDelegatedProperty(property: FirProperty): IrLocalDelegatedPropertySymbol? {
        return delegatedPropertyCache[property]
    }

    fun putDelegatedProperty(firProperty: FirProperty, irPropertySymbol: IrLocalDelegatedPropertySymbol) {
        delegatedPropertyCache[firProperty] = irPropertySymbol
    }

    fun isEmpty(): Boolean {
        return parameterCache.isEmpty()
                && variableCache.isEmpty()
                && localFunctionCache.isEmpty()
                && delegatedPropertyCache.isEmpty()
    }

    // Should be updated respectively when adding new properties
    fun cloneFilteringSymbols(filterOutSymbols: Set<FirBasedSymbol<*>>): Fir2IrScopeCache {
        return Fir2IrScopeCache(
            filterOutSymbolsFromCache(parameters, filterOutSymbols),
            filterOutSymbolsFromCache(variableCache, filterOutSymbols),
            filterOutSymbolsFromCache(localFunctionCache, filterOutSymbols),
            filterOutSymbolsFromCache(delegatedPropertyCache, filterOutSymbols)
        )
    }

    fun clear() {
        parameterCache.clear()
        variableCache.clear()
        localFunctionCache.clear()
        delegatedPropertyCache.clear()
    }
}
