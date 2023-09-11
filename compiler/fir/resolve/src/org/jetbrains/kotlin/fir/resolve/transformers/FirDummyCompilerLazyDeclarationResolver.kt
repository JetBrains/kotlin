/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

object FirDummyCompilerLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: FirResolvePhase) {}
    override fun finishResolvingPhase(phase: FirResolvePhase) {}

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {}
    override fun lazyResolveToPhaseWithCallableMembers(symbol: FirClassSymbol<*>, toPhase: FirResolvePhase) {}
    override fun lazyResolveToPhaseRecursively(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {}
}
