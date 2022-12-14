/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.Name

internal class KtFirMemberFunctionSymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val name: Name,
    private val signature: FirCallableSignature,
    isStatic: Boolean,
) : KtFirMemberSymbolPointer<KtFunctionSymbol>(ownerPointer, isStatic) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtFunctionSymbol? {
        val firFunction = candidates.findDeclarationWithSignature<FirSimpleFunction>(signature) {
            processFunctionsByName(name, it)
        } ?: return null
        return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firFunction.symbol)
    }
}
