/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.Name

internal class KtFirMemberPropertySymbolPointer(
    ownerPointer: KtSymbolPointer<KtSymbolWithMembers>,
    private val name: Name,
    private val signature: FirCallableSignature,
    isStatic: Boolean,
) : KtFirMemberSymbolPointer<KtKotlinPropertySymbol>(ownerPointer, isStatic) {
    override fun KtFirAnalysisSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KtKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignature<FirProperty>(signature) {
            processPropertiesByName(name, it)
        } ?: return null

        return firSymbolBuilder.variableLikeBuilder.buildVariableSymbol(firProperty.symbol) as? KtKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KtSymbolPointer<KtSymbol>): Boolean = this === other ||
            other is KtFirMemberPropertySymbolPointer &&
            other.name == name &&
            other.signature == signature &&
            hasTheSameOwner(other)
}
