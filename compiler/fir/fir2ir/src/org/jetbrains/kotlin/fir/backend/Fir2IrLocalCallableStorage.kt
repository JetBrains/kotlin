/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol

class Fir2IrLocalCallableStorage(initialStack: List<Fir2IrScopeCache>) {
    private val cacheStack = mutableListOf<Fir2IrScopeCache>()

    init {
        cacheStack.addAll(initialStack)
    }

    fun enterCallable() {
        cacheStack += Fir2IrScopeCache()
    }

    @LeakedDeclarationCaches
    val lastCache: Fir2IrScopeCache
        get() = cacheStack.last()

    fun leaveCallable() {
        cacheStack.last().clear()
        cacheStack.removeAt(cacheStack.size - 1)
    }

    fun getParameter(parameter: FirValueParameter): IrValueParameterSymbol? {
        for (cache in cacheStack.asReversed()) {
            val local = cache.getParameter(parameter)
            if (local != null) return local
        }
        return null
    }

    fun getVariable(variable: FirVariable): IrVariableSymbol? {
        return last { getVariable(variable) }
    }

    fun getLocalFunctionSymbol(localFunction: FirFunction): IrSimpleFunctionSymbol? {
        return last { getLocalFunction(localFunction) }
    }

    fun getDelegatedProperty(property: FirProperty): IrLocalDelegatedPropertySymbol? {
        return last { getDelegatedProperty(property) }
    }

    private inline fun <T> last(getter: Fir2IrScopeCache.() -> T?): T? {
        for (cache in cacheStack.asReversed()) {
            cache.getter()?.let { return it }
        }
        return null
    }

    fun putParameter(firParameter: FirValueParameter, irParameterSymbol: IrValueParameterSymbol) {
        cacheStack.last().putParameter(firParameter, irParameterSymbol)
    }

    fun putVariable(firVariable: FirVariable, irVariableSymbol: IrVariableSymbol) {
        cacheStack.last().putVariable(firVariable, irVariableSymbol)
    }

    fun putLocalFunction(firFunction: FirFunction, irFunctionSymbol: IrSimpleFunctionSymbol) {
        cacheStack.last().putLocalFunction(firFunction, irFunctionSymbol)
    }

    fun putDelegatedProperty(firProperty: FirProperty, irPropertySymbol: IrLocalDelegatedPropertySymbol) {
        cacheStack.last().putDelegatedProperty(firProperty, irPropertySymbol)
    }
}
