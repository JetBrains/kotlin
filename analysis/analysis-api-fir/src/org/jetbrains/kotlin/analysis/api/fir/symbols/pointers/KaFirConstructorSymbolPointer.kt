/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.scopes.FirScope

internal class KaFirConstructorSymbolPointer(
    ownerPointer: KaSymbolPointer<KaDeclarationContainerSymbol>,
    private val isPrimary: Boolean,
    private val signature: FirCallableSignature,
    originalSymbol: KaConstructorSymbol?,
) : KaFirMemberSymbolPointer<KaConstructorSymbol>(ownerPointer, originalSymbol = originalSymbol) {
    override fun KaFirSession.chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        firSession: FirSession,
    ): KaConstructorSymbol? {
        val firConstructor = candidates.findDeclarationWithSignature<FirConstructor>(signature) {
            processDeclaredConstructors(it)
        } ?: return null

        if (firConstructor.isPrimary != isPrimary) return null
        return firSymbolBuilder.functionBuilder.buildConstructorSymbol(firConstructor.symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = other === this ||
            other is KaFirConstructorSymbolPointer &&
            other.signature == signature &&
            other.isPrimary == isPrimary &&
            hasTheSameOwner(other)
}
