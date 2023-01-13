/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId

internal abstract class KtTopLevelCallableSymbolPointer<S : KtCallableSymbol>(
    private val callableId: CallableId
) : KtSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KtAnalysisSession.restoreSymbol")
    final override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        require(analysisSession is KtFirAnalysisSession)
        val candidates = analysisSession.getCallableSymbols(callableId)
        if (candidates.isEmpty()) return null
        val session = candidates.first().fir.moduleData.session
        return analysisSession.chooseCandidateAndCreateSymbol(candidates, session)
    }

    protected abstract fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession
    ): S?

    abstract override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean
    protected fun hasTheSameOwner(other: KtTopLevelCallableSymbolPointer<*>): Boolean = other.callableId == callableId
}

private fun KtFirAnalysisSession.getCallableSymbols(callableId: CallableId) =
    firResolveSession.useSiteFirSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)

