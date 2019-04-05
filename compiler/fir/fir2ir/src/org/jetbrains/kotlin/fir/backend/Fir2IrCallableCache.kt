/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirVariable
import org.jetbrains.kotlin.ir.declarations.*

class Fir2IrCallableCache {
    private val parameterCache = mutableMapOf<FirValueParameter, IrValueParameter>()

    private val variableCache = mutableMapOf<FirVariable, IrVariable>()

    private val localClassCache = mutableMapOf<FirClass, IrClass>()

    private val localFunctionCache = mutableMapOf<FirFunction, IrSimpleFunction>()

    fun getParameter(parameter: FirValueParameter): IrValueParameter? = parameterCache[parameter]

    fun putParameter(firParameter: FirValueParameter, irParameter: IrValueParameter) {
        parameterCache[firParameter] = irParameter
    }

    fun getVariable(variable: FirVariable): IrVariable? = variableCache[variable]

    fun putVariable(firVariable: FirVariable, irVariable: IrVariable) {
        variableCache[firVariable] = irVariable
    }

    fun getLocalClass(localClass: FirClass): IrClass? = localClassCache[localClass]

    fun putLocalClass(localClass: FirClass, irClass: IrClass) {
        require(localClass !is FirRegularClass || localClass.visibility == Visibilities.LOCAL)
        localClassCache[localClass] = irClass
    }

    fun getLocalFunction(localFunction: FirFunction): IrSimpleFunction? = localFunctionCache[localFunction]

    fun putLocalFunction(localFunction: FirFunction, irFunction: IrSimpleFunction) {
        require(localFunction !is FirNamedFunction || localFunction.visibility == Visibilities.LOCAL)
        localFunctionCache[localFunction] = irFunction
    }

    fun clear() {
        parameterCache.clear()
        variableCache.clear()
        localClassCache.clear()
        localFunctionCache.clear()
    }
}