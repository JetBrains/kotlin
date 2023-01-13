/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.ir.declarations.*

class Fir2IrLocalCallableStorage {

    private val cacheStack = mutableListOf<Fir2IrScopeCache>()

    fun enterCallable() {
        cacheStack += Fir2IrScopeCache()
    }

    fun leaveCallable() {
        cacheStack.last().clear()
        cacheStack.removeAt(cacheStack.size - 1)
    }

    fun getParameter(parameter: FirValueParameter): IrValueParameter? {
        for (cache in cacheStack.asReversed()) {
            val local = cache.getParameter(parameter)
            if (local != null) return local
        }
        return null
    }

    fun getVariable(variable: FirVariable): IrVariable? =
        last { getVariable(variable) }

    fun getLocalFunction(localFunction: FirFunction): IrSimpleFunction? =
        last { getLocalFunction(localFunction) }

    fun getDelegatedProperty(property: FirProperty): IrLocalDelegatedProperty? =
        last { getDelegatedProperty(property) }

    private inline fun <T> last(getter: Fir2IrScopeCache.() -> T?): T? {
        for (cache in cacheStack.asReversed()) {
            cache.getter()?.let { return it }
        }
        return null
    }

    fun putParameter(firParameter: FirValueParameter, irParameter: IrValueParameter) {
        cacheStack.last().putParameter(firParameter, irParameter)
    }

    fun putVariable(firVariable: FirVariable, irVariable: IrVariable) {
        cacheStack.last().putVariable(firVariable, irVariable)
    }

    fun putLocalFunction(firFunction: FirFunction, irFunction: IrSimpleFunction) {
        cacheStack.last().putLocalFunction(firFunction, irFunction)
    }

    fun putDelegatedProperty(firProperty: FirProperty, irProperty: IrLocalDelegatedProperty) {
        cacheStack.last().putDelegatedProperty(firProperty, irProperty)
    }
}
