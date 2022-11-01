/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver


class FirCompilerLazyDeclarationResolverWithPhaseChecking : FirLazyDeclarationResolver() {
    private var currentTransformerPhase: FirResolvePhase? = null
    private var isEnabled = true

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase)
    }

    override fun startResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase == null)
        currentTransformerPhase = phase
    }

    override fun finishResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase == phase)
        currentTransformerPhase = null
    }

    override fun disableLazyResolveContractChecks() {
        isEnabled = false
    }

    override fun enableLazyResolveContractsChecks() {
        isEnabled = true
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase) {
        if (!isEnabled) return

        val currentPhase = currentTransformerPhase
            ?: error("Current phase is not set, please call ${this::startResolvingPhase.name} before starting transforming the file")

        if (requestedPhase >= currentPhase) {
            throw FirLazyResolveContractViolationException(
                """`lazyResolveToPhase($requestedPhase)` cannot be called from a transformer with a phase $currentPhase.
                    `lazyResolveToPhase` can be called only from a transformer with a phase which is strictly greater than a requested phase;
                     i.e., `lazyResolveToPhase(A)` may be only called from a lazy transformer with a phase B, where A < B. This is a contract of lazy resolve""".trimIndent()
            )
        }
    }
}

class FirLazyResolveContractViolationException(message: String) : IllegalStateException(message)

