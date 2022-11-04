/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.ir.util.IdSignature

internal class KtFirConstructorSymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val isPrimary: Boolean,
    private val signature: IdSignature,
) : KtFirMemberSymbolPointer<KtConstructorSymbol>(ownerPointer) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KtConstructorSymbol? {
        val firConstructor = candidates.findDeclarationWithSignature<FirConstructor>(signature, firSession) {
            processDeclaredConstructors(it)
        } ?: return null

        if (firConstructor.isPrimary != isPrimary) return null
        return firSymbolBuilder.functionLikeBuilder.buildConstructorSymbol(firConstructor.symbol)
    }
}
