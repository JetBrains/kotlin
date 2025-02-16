/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.Name

internal class KaFirMemberPropertySymbolPointer(
    ownerPointer: KaSymbolPointer<KaDeclarationContainerSymbol>,
    private val name: Name,
    private val signature: FirCallableSignature,
    isStatic: Boolean,
    originalSymbol: KaKotlinPropertySymbol?,
) : KaFirMemberSymbolPointer<KaKotlinPropertySymbol>(ownerPointer, isStatic, originalSymbol) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession
    ): KaKotlinPropertySymbol? {
        val firProperty = candidates.findDeclarationWithSignature<FirProperty>(signature) {
            processPropertiesByName(name, it)
        } ?: return null

        return firSymbolBuilder.variableBuilder.buildVariableSymbol(firProperty.symbol) as? KaKotlinPropertySymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirMemberPropertySymbolPointer &&
            other.name == name &&
            other.signature == signature &&
            hasTheSameOwner(other)
}
