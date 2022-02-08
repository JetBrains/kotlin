/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope

/**
 * Returns the list of functions that overridden by given
 */
fun FirSimpleFunction.lowestVisibilityAmongOverrides(
    containingClass: FirClass,
    session: FirSession,
    scopeSession: ScopeSession
): Visibility {
    val firTypeScope = containingClass.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)
    var visibility = visibility

    // required; otherwise processOverriddenFunctions()
    // will process nothing
    firTypeScope.processFunctionsByName(symbol.fir.name) { }

    firTypeScope.processOverriddenFunctions(symbol) {
        visibility = it.fir.visibility
        ProcessorAction.NEXT
    }

    return visibility
}
