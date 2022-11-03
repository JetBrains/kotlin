/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.name.Name

internal class KtFirMemberTypeAliasSymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val name: Name,
) : KtFirMemberSymbolPointer<KtTypeAliasSymbol>(ownerPointer) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtTypeAliasSymbol? {
        val result = candidates.findClassifier<FirTypeAliasSymbol>(name) ?: return null
        return firSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(result)
    }
}
