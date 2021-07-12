/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

    private fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        return overrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)
    }

    private fun isOverriddenProperty(overrideCandidate: FirCallableDeclaration, baseDeclaration: FirProperty): Boolean {
        return overrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
    }

    protected fun similarFunctionsOrBothProperties(
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

    // Receiver is super-type function here
    protected open fun FirCallableSymbol<*>.getOverridden(overrideCandidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        if (overrideByBase.containsKey(this)) return overrideByBase[this]

        val baseDeclaration = (this as FirBasedSymbol<*>).fir as FirCallableDeclaration
        val override = overrideCandidates.firstOrNull {
            val overrideCandidate = (it as FirBasedSymbol<*>).fir as FirCallableDeclaration
            baseDeclaration.modality != Modality.FINAL && similarFunctionsOrBothProperties(overrideCandidate, baseDeclaration)
        } // TODO: two or more overrides for one fun?
        overrideByBase[this] = override
        return override
    }

}
