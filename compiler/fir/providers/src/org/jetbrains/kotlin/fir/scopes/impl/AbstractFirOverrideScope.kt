/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

abstract class AbstractFirOverrideScope(
    val session: FirSession,
    protected val overrideChecker: FirOverrideChecker
) : FirTypeScope() {
    //base symbol as key, overridden as value
    val overrideByBase = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>?>()

    // Receiver is super-type function here
    protected open fun FirCallableSymbol<*>.getOverridden(overrideCandidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        val overrideByBaseItem = overrideByBase[this]
        if (overrideByBaseItem != null) return overrideByBaseItem

        val baseDeclaration = (this as FirBasedSymbol<*>).fir as FirCallableDeclaration
        val override = overrideCandidates.firstOrNull {
            val overrideCandidate = (it as FirBasedSymbol<*>).fir as FirCallableDeclaration
            baseDeclaration.modality != Modality.FINAL && overrideChecker.similarFunctionsOrBothProperties(
                overrideCandidate,
                baseDeclaration
            )
        } // TODO: two or more overrides for one fun?
        overrideByBase[this] = override
        return override
    }

}

internal fun FirOverrideChecker.similarFunctionsOrBothProperties(
    overrideCandidate: FirCallableDeclaration,
    baseDeclaration: FirCallableDeclaration
): Boolean {
    return when (overrideCandidate) {
        is FirSimpleFunction -> when (baseDeclaration) {
            is FirSimpleFunction -> isOverriddenFunction(overrideCandidate, baseDeclaration)
            is FirProperty -> isOverriddenProperty(overrideCandidate, baseDeclaration)
            else -> false
        }
        is FirConstructor -> false
        is FirProperty -> baseDeclaration is FirProperty && isOverriddenProperty(overrideCandidate, baseDeclaration)
        is FirField -> baseDeclaration is FirField
        else -> error("Unknown fir callable type: $overrideCandidate, $baseDeclaration")
    }
}

fun FirOverrideChecker.similarFunctionsOrBothProperties(
    overrideCandidate: FirCallableSymbol<*>,
    baseDeclaration: FirCallableSymbol<*>
): Boolean {
    return similarFunctionsOrBothProperties(overrideCandidate.fir, baseDeclaration.fir)
}
