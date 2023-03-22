/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractImportingScope

class FirDefaultParametersResolver : FirSessionComponent {
    fun declaresDefaultValue(
        session: FirSession,
        scopeSession: ScopeSession,
        valueParameter: FirValueParameter,
        function: FirFunction,
        originScope: FirScope?,
        index: Int,
    ): Boolean {
        if (valueParameter.defaultValue != null || function.symbol.getSingleCompatibleExpectForActualOrNull().containsDefaultValue(index)) {
            return true
        }
        if (function !is FirSimpleFunction) return false
        val symbol = function.symbol
        val typeScope = when (originScope) {
            is FirTypeScope -> originScope
            // imported from object case
            is FirAbstractImportingScope -> {
                val containingClass = function.getContainingClass(session) ?: return false
                containingClass.scopeForClass(
                    ConeSubstitutor.Empty,
                    session,
                    scopeSession,
                    containingClass.symbol.toLookupTag(),
                    memberRequiredPhase = null,
                )
            }
            else -> return false
        }
        var result = false

        typeScope.processOverriddenFunctions(symbol) { overridden ->
            if (overridden.containsDefaultValue(index) ||
                overridden.getSingleCompatibleExpectForActualOrNull().containsDefaultValue(index)
            ) {
                result = true
                return@processOverriddenFunctions ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }
}

internal val FirSession.defaultParameterResolver: FirDefaultParametersResolver by FirSession.sessionComponentAccessor()
