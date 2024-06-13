/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId

internal class KaFirTopLevelFunctionSymbolPointer(
    callableId: CallableId,
    private val signature: FirCallableSignature,
) : KaTopLevelCallableSymbolPointer<KaFunctionSymbol>(callableId) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession
    ): KaFunctionSymbol? {
        val firFunction = candidates.findDeclarationWithSignatureBySymbols<FirSimpleFunction>(signature) ?: return null
        return firSymbolBuilder.functionLikeBuilder.buildFunctionSymbol(firFunction.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirTopLevelFunctionSymbolPointer &&
            other.signature == signature &&
            hasTheSameOwner(other)
}
