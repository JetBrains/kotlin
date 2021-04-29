/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId

internal abstract class KtTopLevelCallableSymbolPointer<S : KtCallableSymbol>(
    private val callableId: CallableId
) : KtSymbolPointer<S>() {
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
}

private fun KtFirAnalysisSession.getCallableSymbols(callableId: CallableId) =
    firResolveState.rootModuleSession.symbolProvider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)

