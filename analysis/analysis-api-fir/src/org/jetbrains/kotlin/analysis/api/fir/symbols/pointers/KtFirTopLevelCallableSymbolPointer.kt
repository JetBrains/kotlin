/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

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
    private val callableId: CallableId
) : KaSymbolPointer<S>() {
    @Deprecated("Consider using org.jetbrains.kotlin.analysis.api.KaSession.restoreSymbol")
    final override fun restoreSymbol(analysisSession: KaSession): S? {
        require(analysisSession is KaFirSession)
        val candidates = analysisSession.getCallableSymbols(callableId)
        if (candidates.isEmpty()) return null
        val session = candidates.first().fir.moduleData.session
        return analysisSession.chooseCandidateAndCreateSymbol(candidates, session)
    }

    protected abstract fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession
    ): S?

    abstract override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean
    protected fun hasTheSameOwner(other: KaTopLevelCallableSymbolPointer<*>): Boolean = other.callableId == callableId
}

private fun KaFirSession.getCallableSymbols(callableId: CallableId) =
    useSiteSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)

