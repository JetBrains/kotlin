/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

internal abstract class KtFirMemberSymbolPointer<S : KtSymbol>(
    private val ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val isStatic: Boolean = false,
) : KtSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    final override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        require(analysisSession is KtFirAnalysisSession)
        val scope = with(analysisSession) {
            val ownerSymbol = ownerPointer.restoreSymbol() ?: return null
            val owner = ownerSymbol.firSymbol as? FirClassSymbol ?: return null
            getSearchScope(owner)
        } ?: return null

        return analysisSession.chooseCandidateAndCreateSymbol(scope, analysisSession.useSiteSession)
    }

    protected abstract fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(candidates: FirScope, firSession: FirSession): S?

    context(KtFirAnalysisSession)
    protected open fun getSearchScope(owner: FirClassSymbol<*>): FirScope? {
        val firSession = useSiteSession
        val scopeSession = getScopeSessionFor(firSession)
        return if (isStatic) {
            val firClass = owner.fir
            firClass.scopeProvider.getStaticMemberScopeForCallables(firClass, firSession, scopeSession)
        } else {
            owner.unsubstitutedScope(
                useSiteSession = firSession,
                scopeSession = scopeSession,
                withForcedTypeCalculator = false,
                memberRequiredPhase = FirResolvePhase.STATUS,
            )
        }
    }

    abstract override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean
    protected fun hasTheSameOwner(other: KtFirMemberSymbolPointer<*>): Boolean =
        other.isStatic == isStatic && other.ownerPointer.pointsToTheSameSymbolAs(ownerPointer)
}

context(KtAnalysisSession)
internal inline fun <reified T : KtSymbol> KtSymbol.requireOwnerPointer(): KtSymbolPointer<T> {
    val symbolWithMembers = getContainingSymbol()
    requireNotNull(symbolWithMembers) { "should present for member declaration" }
    requireIsInstance<T>(symbolWithMembers)

    @Suppress("UNCHECKED_CAST")
    return symbolWithMembers.createPointer() as KtSymbolPointer<T>
}
