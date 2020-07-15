/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext

val FirSession.typeContext: ConeInferenceContext
    get() = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = false, this)

/**
 * Returns the list of functions that overridden by given
 */
fun FirSimpleFunction.lowestVisibilityAmongOverrides(
    containingClass: FirClass<*>,
    session: FirSession,
    scopeSession: ScopeSession
): Visibility {
    val firTypeScope = containingClass.unsubstitutedScope(session, scopeSession) as FirTypeScope
    var visibility = visibility

    // required; otherwise processOverriddenFunctions()
    // will process nothing
    firTypeScope.processFunctionsByName(symbol.fir.name) { }

    firTypeScope.processOverriddenFunctions(symbol) {
        val overriddenVisibility = when (val fir = it.fir) {
            is FirMemberDeclaration -> fir.visibility
            is FirPropertyAccessor -> fir.visibility
            else -> null
        }

        overriddenVisibility?.let { that ->
            visibility = that
        }

        ProcessorAction.NEXT
    }

    return visibility
}