/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner

fun <D> AbstractFirBasedSymbol<D>.phasedFir(
    session: FirSession,
    requiredPhase: FirResolvePhase = FirResolvePhase.DECLARATIONS
): D where D : FirDeclaration, D : FirSymbolOwner<D> {
    val result = this.fir
    val availablePhase = result.resolvePhase
    if (availablePhase < requiredPhase) {
        val provider = FirProvider.getInstance(session)
        val containingFile = when (this) {
            is FirSyntheticFunctionSymbol -> file
            is ConeCallableSymbol -> provider.getFirCallableContainerFile(this)
            is ConeClassLikeSymbol -> provider.getFirClassifierContainerFile(this)
            else -> null
        } ?: throw AssertionError("Cannot get container file by symbol: $this (${result.render()})")
        containingFile.runResolve(toPhase = requiredPhase, fromPhase = availablePhase)
    }
    return result
}
