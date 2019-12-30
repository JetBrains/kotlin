/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

fun <D> AbstractFirBasedSymbol<D>.phasedFir(
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): D where D : FirDeclaration, D : FirSymbolOwner<D> {
    val result = this.fir
    val availablePhase = result.resolvePhase
    if (availablePhase < requiredPhase) {
        // NB: we should use session from symbol here, not transformer session (important for IDE)
        val provider = fir.session.firProvider

        require(provider.isPhasedFirAllowed) {
            "Incorrect resolvePhase: actual: $availablePhase, expected: $requiredPhase\n For: ${fir.render()}"
        }

        val containingFile = when (this) {
            is FirCallableSymbol<*> -> provider.getFirCallableContainerFile(this)
            is FirClassLikeSymbol<*> -> provider.getFirClassifierContainerFile(this)
            else -> null
        }
            ?: throw AssertionError("Cannot get container file by symbol: $this (${result.render()})")
        containingFile.runResolve(toPhase = requiredPhase, fromPhase = availablePhase)
    }
    return result
}
