/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.Name

internal class KaFirMemberFunctionSymbolPointer(
    ownerPointer: KaSymbolPointer<KaDeclarationContainerSymbol>,
    private val name: Name,
    private val signature: FirCallableSignature,
    isStatic: Boolean,
    originalSymbol: KaNamedFunctionSymbol?,
) : KaFirMemberSymbolPointer<KaNamedFunctionSymbol>(ownerPointer, isStatic, originalSymbol) {
    context(analysisSession: KaFirSession)
    override fun chooseCandidateAndCreateSymbol(
        candidates: FirScope,
        owner: FirClassSymbol<*>,
    ): KaNamedFunctionSymbol? {
        val symbol = candidates.findMemberWithSignature<FirNamedFunction>(owner, signature) {
            processFunctionsByName(name, it)
        }

        return symbol as? KaNamedFunctionSymbol
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFirMemberFunctionSymbolPointer &&
            other.name == name &&
            other.signature == signature &&
            hasTheSameOwner(other)
}
