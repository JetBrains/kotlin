/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException


class FirCompilerLazyDeclarationResolverWithPhaseChecking : FirLazyDeclarationResolver() {
    private var currentTransformerPhase: FirResolvePhase? = null

    private val exceptions = mutableListOf<FirLazyResolveContractViolationException>()

    fun getContractViolationExceptions(): List<FirLazyResolveContractViolationException> =
        exceptions

    override fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase)
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase)
    }

    override fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
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

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase) {
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
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
        }
    }
}
