/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId

internal abstract class KaTopLevelCallableSymbolPointer<S : KaCallableSymbol>(
    private val callableId: CallableId,
    originalSymbol: S?,
) : KaFirCachedSymbolPointer<S>(originalSymbol) {
    @KaImplementationDetail
    context(session: KaSession)
    final override fun restoreIfNotCached(): S? {
        require(session is KaFirSession)
        val candidates = getCallableSymbols(callableId, analysisSession = session)
        if (candidates.isEmpty()) return null
        val candidateSession = candidates.first().fir.moduleData.session
        return chooseCandidateAndCreateSymbol(candidates, candidateSession, analysisSession = session)
    }

    context(analysisSession: KaFirSession)
    protected abstract fun chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession
    ): S?

    abstract override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean
    protected fun hasTheSameOwner(other: KaTopLevelCallableSymbolPointer<*>): Boolean = other.callableId == callableId
}

context(analysisSession: KaFirSession)
private fun getCallableSymbols(callableId: CallableId) =
    analysisSession.firSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)

