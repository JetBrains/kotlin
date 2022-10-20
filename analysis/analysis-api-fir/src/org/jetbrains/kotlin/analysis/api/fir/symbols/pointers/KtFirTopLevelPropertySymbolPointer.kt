/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.CallableId

internal class KtFirTopLevelPropertySymbolPointer(callableId: CallableId, private val signature: IdSignature) :
    KtTopLevelCallableSymbolPointer<KtKotlinPropertySymbol>(callableId) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: Collection<FirCallableSymbol<*>>,
        firSession: FirSession,
    ): KtKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignatureBySymbols<FirProperty>(signature, firSession) ?: return null
        return firSymbolBuilder.variableLikeBuilder.buildPropertySymbol(firProperty.symbol) as? KtKotlinPropertySymbol
    }
}
