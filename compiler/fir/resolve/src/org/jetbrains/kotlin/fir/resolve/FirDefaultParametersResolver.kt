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
import org.jetbrains.kotlin.fir.scopes.impl.FirActualizingScope

class FirDefaultParametersResolver : FirSessionComponent {
    fun declaresDefaultValue(
        session: FirSession,
        scopeSession: ScopeSession,
        function: FirFunction,
        originScope: FirScope?,
        index: Int,
    ): Boolean {
        if (function.itOrExpectHasDefaultParameterValue(index)) return true
        if (function !is FirSimpleFunction) return false
        val symbol = function.symbol
        val typeScope = when (originScope) {
            is FirTypeScope -> originScope
            // Handle other scopes, including importing from an object
            // TODO: probably it makes sense to refactor it (KT-70016)
            is FirActualizingScope,
            is FirAbstractImportingScope -> {
                val containingClass = function.getContainingClass() ?: return false
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
            if (overridden.fir.itOrExpectHasDefaultParameterValue(index)) {
                result = true
                return@processOverriddenFunctions ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }
}

internal val FirSession.defaultParameterResolver: FirDefaultParametersResolver by FirSession.sessionComponentAccessor()
