/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions

class FirDefaultParametersResolver : FirSessionComponent {
    fun declaresDefaultValue(
        valueParameter: FirValueParameter,
        function: FirFunction<*>,
        originScope: FirScope?,
        index: Int,
    ): Boolean {
        if (valueParameter.defaultValue != null) return true
        if (originScope !is FirTypeScope) return false
        var result = false

        originScope.processOverriddenFunctions(function.symbol) { overridden ->
            if (overridden.fir.valueParameters[index].defaultValue != null) {
                result = true
                return@processOverriddenFunctions ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }
}

internal val FirSession.defaultParameterResolver: FirDefaultParametersResolver by FirSession.sessionComponentAccessor()
