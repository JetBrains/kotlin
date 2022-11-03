/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.Name

internal class KtFirMemberClassOrObjectSymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val name: Name,
) : KtFirMemberSymbolPointer<KtNamedClassOrObjectSymbol>(ownerPointer) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtNamedClassOrObjectSymbol? {
        val result = candidates.findClassifier<FirRegularClassSymbol>(name) ?: return null
        return firSymbolBuilder.classifierBuilder.buildNamedClassOrObjectSymbol(result)
    }
}
