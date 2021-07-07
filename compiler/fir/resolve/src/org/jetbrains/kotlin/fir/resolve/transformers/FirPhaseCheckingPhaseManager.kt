/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirPhaseManager

object FirPhaseCheckingPhaseManager : FirPhaseManager() {
    override fun ensureResolved(symbol: FirBasedSymbol<*>, requiredPhase: FirResolvePhase) {
        val fir = symbol.fir
        val availablePhase = fir.resolvePhase
        require(availablePhase >= requiredPhase) {
            "Expected at least $requiredPhase for $symbol but was $availablePhase\n{${fir.render(renderMode)}"
        }
    }

    private val renderMode = FirRenderer.RenderMode.WithResolvePhases.copy(renderDeclarationOrigin = true)
}
