/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol


class FirCompilerLazyDeclarationResolverWithPhaseChecking : FirLazyDeclarationResolver() {
    private var currentTransformerPhase: FirResolvePhase? = null

    private val exceptions = mutableListOf<FirLazyResolveContractViolationException>()

    fun getContractViolationExceptions(): List<FirLazyResolveContractViolationException> =
        exceptions

    override fun lazyResolveToPhase(symbol: FirBasedSymbol<*>, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(symbol, toPhase)
    }

    override fun lazyResolveToPhaseWithCallableMembers(symbol: FirClassSymbol<*>, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(symbol, toPhase)
    }

    override fun startResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase == null)
        currentTransformerPhase = phase
    }

    override fun finishResolvingPhase(phase: FirResolvePhase) {
        check(currentTransformerPhase == phase)
        currentTransformerPhase = null
    }

    private fun checkIfCanLazyResolveToPhase(symbol: FirBasedSymbol<*>, requestedPhase: FirResolvePhase) {
        if (!lazyResolveContractChecksEnabled) return

        val currentPhase = currentTransformerPhase
            ?: error("Current phase is not set, please call ${this::startResolvingPhase.name} before starting transforming the file")

        // TODO: symbol current phase can already be requestedPhase or more.
        // This case is not a violation of any contract
        // However, now we keep more strict invariant here,
        // because we don't want to hide some cases when transformers violate phase contract directly
        // but due to usage of already resolved stdlib classes we don't see it
        if (requestedPhase >= currentPhase) {
            exceptions += FirLazyResolveContractViolationException(
                """`lazyResolveToPhase($requestedPhase)` cannot be called from a transformer with a phase $currentPhase.
                    lazyResolveToPhase was called on a $symbol.
                    `lazyResolveToPhase` can be called only from a transformer with a phase which is strictly greater than a requested phase;
                     i.e., `lazyResolveToPhase(A)` may be only called from a lazy transformer with a phase B, where A < B. This is a contract of lazy resolve""".trimIndent()
            )
        }
    }
}

class FirLazyResolveContractViolationException(message: String) : IllegalStateException(message)
