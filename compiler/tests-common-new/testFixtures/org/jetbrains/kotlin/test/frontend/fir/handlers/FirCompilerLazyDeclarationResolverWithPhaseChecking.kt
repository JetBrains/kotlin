/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.isItAllowedToCallLazyResolveTo
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.FirLazyDeclarationResolver
import org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.testInfraError


class FirCompilerLazyDeclarationResolverWithPhaseChecking : FirLazyDeclarationResolver() {
    private var currentTransformerPhase: FirResolvePhase? = null

    private val exceptions = mutableListOf<FirLazyResolveContractViolationException>()

    fun getContractViolationExceptions(): List<FirLazyResolveContractViolationException> =
        exceptions

    override fun lazyResolveToPhase(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: FirClass, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, clazz.resolvePhase)
    }

    override fun lazyResolveToPhaseRecursively(element: FirElementWithResolveState, toPhase: FirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    override fun startResolvingPhase(phase: FirResolvePhase) {
        checkTestInfrastructure(currentTransformerPhase == null) { "currentTransformerPhase must be null"}
        currentTransformerPhase = phase
    }

    override fun finishResolvingPhase(phase: FirResolvePhase) {
        checkTestInfrastructure(currentTransformerPhase == phase)  { "currentTransformerPhase must be $phase"}
        currentTransformerPhase = null
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase, elementPhase: FirResolvePhase) {
        if (!lazyResolveContractChecksEnabled || elementPhase >= requestedPhase) return

        val currentPhase = currentTransformerPhase
            ?: testInfraError("Current phase is not set, please call ${this::startResolvingPhase.name} before starting transforming the file")

        // This case is not a violation of any contract
        // However, now we keep more strict invariant here,
        // because we don't want to hide some cases when transformers violate phase contract directly
        // but due to usage of already resolved stdlib classes we don't see it
        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            exceptions += FirLazyResolveContractViolationException(
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
        }
    }
}
