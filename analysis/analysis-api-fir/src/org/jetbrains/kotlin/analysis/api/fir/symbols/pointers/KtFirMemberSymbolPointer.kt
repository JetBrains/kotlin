/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

internal abstract class KtFirMemberSymbolPointer<S : KtSymbol>(
    private val ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
) : KtSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    final override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        require(analysisSession is KtFirAnalysisSession)
        val ownerSymbol = with(analysisSession) {
            ownerPointer.restoreSymbol()
        }

        val owner = ownerSymbol?.firSymbol as? FirClassSymbol ?: return null
        val firSession = analysisSession.useSiteSession
        val scope = owner.unsubstitutedScope(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
            withForcedTypeCalculator = false,
        )

        return analysisSession.chooseCandidateAndCreateSymbol(scope, firSession)
    }

    protected abstract fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(candidates: FirScope, firSession: FirSession): S?
}

context(KtAnalysisSession)
internal inline fun <reified T : KtSymbol> KtSymbol.requireOwnerPointer(): KtSymbolPointer<T> {
    val symbolWithMembers = getContainingSymbol()
    requireNotNull(symbolWithMembers) { "should present for member declaration" }
    requireIsInstance<T>(symbolWithMembers)

    @Suppress("UNCHECKED_CAST")
    return symbolWithMembers.createPointer() as KtSymbolPointer<T>
}

internal inline fun <reified T : KtSymbol> KtFirSymbol<*>.requireOwnerPointer(): KtSymbolPointer<T> {
    return analyze(firResolveSession.useSiteKtModule) {
        requireOwnerPointer<T>()
    }
}
