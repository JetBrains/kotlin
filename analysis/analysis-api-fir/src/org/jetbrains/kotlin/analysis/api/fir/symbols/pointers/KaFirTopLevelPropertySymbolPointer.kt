/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.CallableId

internal class KaFirTopLevelPropertySymbolPointer(callableId: CallableId, private val signature: FirCallableSignature) :
    KaTopLevelCallableSymbolPointer<KaKotlinPropertySymbol>(callableId) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession,
    ): KaKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignatureBySymbols<FirProperty>(signature) ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildPropertySymbol(firProperty.symbol) as? KaKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirTopLevelPropertySymbolPointer &&
            other.signature == signature &&
            hasTheSameOwner(other)
}
