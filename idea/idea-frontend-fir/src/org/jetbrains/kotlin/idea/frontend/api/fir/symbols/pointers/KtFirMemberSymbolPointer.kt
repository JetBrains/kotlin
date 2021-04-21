/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols.pointers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.ClassId

internal abstract class KtFirMemberSymbolPointer<S : KtSymbol>(
    private val ownerClassId: ClassId,
) : KtSymbolPointer<S>() {
    final override fun restoreSymbol(analysisSession: KtAnalysisSession): S? {
        require(analysisSession is KtFirAnalysisSession)
        val owner = analysisSession.getClassLikeSymbol(ownerClassId) as? FirRegularClass
            ?: return null
        val scope = owner.unsubstitutedScope(
            analysisSession.firResolveState.rootModuleSession,
            ScopeSession(),
            withForcedTypeCalculator = false
        )
        return analysisSession.chooseCandidateAndCreateSymbol(scope, owner.declarationSiteSession)
    }

    protected abstract fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): S?
}

private fun KtFirAnalysisSession.getClassLikeSymbol(classId: ClassId) =
    firResolveState.rootModuleSession.symbolProvider.getClassLikeSymbolByFqName(classId)?.fir

