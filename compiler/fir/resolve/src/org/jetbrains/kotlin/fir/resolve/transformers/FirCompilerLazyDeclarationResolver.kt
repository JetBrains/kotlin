/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver

/**
 * Compiler is non-lazy, so it does nothing.
 */
object FirCompilerLazyDeclarationResolver : FirLazyDeclarationResolver() {
    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
    }
}
