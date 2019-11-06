/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol

abstract class AbstractFirOverrideScope(val session: FirSession, private val overrideChecker: FirOverrideChecker) : FirScope() {
    //base symbol as key, overridden as value
    val overrideByBase = mutableMapOf<FirCallableSymbol<*>, FirCallableSymbol<*>?>()

    private fun isOverriddenFunction(overrideCandidate: FirSimpleFunction, baseDeclaration: FirSimpleFunction): Boolean {
        return overrideChecker.isOverriddenFunction(overrideCandidate, baseDeclaration)
    }

    private fun isOverriddenProperty(overrideCandidate: FirCallableMemberDeclaration<*>, baseDeclaration: FirProperty): Boolean {
        return overrideChecker.isOverriddenProperty(overrideCandidate, baseDeclaration)
    }

    private fun similarFunctionsOrBothProperties(
        overrideCandidate: FirCallableMemberDeclaration<*>,
        baseDeclaration: FirCallableMemberDeclaration<*>
    ): Boolean {
        return when (overrideCandidate) {
            is FirSimpleFunction -> when (baseDeclaration) {
                is FirSimpleFunction -> isOverriddenFunction(overrideCandidate, baseDeclaration)
                is FirProperty -> isOverriddenProperty(overrideCandidate, baseDeclaration)
                else -> false
            }
            is FirConstructor -> false
            is FirProperty -> baseDeclaration is FirProperty && isOverriddenProperty(overrideCandidate, baseDeclaration)
            is FirField -> false
            else -> error("Unknown fir callable type: $overrideCandidate, $baseDeclaration")
        }
    }

    // Receiver is super-type function here
    protected open fun FirCallableSymbol<*>.getOverridden(overrideCandidates: Set<FirCallableSymbol<*>>): FirCallableSymbol<*>? {
        if (overrideByBase.containsKey(this)) return overrideByBase[this]

        val baseDeclaration = (this as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
        val override = overrideCandidates.firstOrNull {
            val overrideCandidate = (it as AbstractFirBasedSymbol<*>).fir as FirCallableMemberDeclaration<*>
            baseDeclaration.modality != Modality.FINAL && similarFunctionsOrBothProperties(overrideCandidate, baseDeclaration)
        } // TODO: two or more overrides for one fun?
        overrideByBase[this] = override
        return override
    }

}