/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.isItAllowedToCallLazyResolveTo
import org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException

/**
 * Checks a lazy resolve call's compliance with [FirResolvePhase] contracts.
 *
 * @see FirResolvePhase
 * @see FirLazyResolveContractViolationException
 */
internal class LLFirLazyResolveContractChecker {
    private val currentTransformerPhase = ThreadLocal.withInitial<FirResolvePhase?> { null }

    inline fun lazyResolveToPhaseInside(phase: FirResolvePhase, resolve: () -> Unit) {
        checkIfCanLazyResolveToPhase(phase)

        val previousPhase = currentTransformerPhase.get()
        currentTransformerPhase.set(phase)
        try {
            resolve()
        } finally {
            currentTransformerPhase.set(previousPhase)
        }
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase) {
        val currentPhase = currentTransformerPhase.get() ?: return

        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            val exception = FirLazyResolveContractViolationException(currentPhase = currentPhase, requestedPhase = requestedPhase)
            if (System.getProperty("kotlin.suppress.lazy.resolve.contract.violation") != null) {
                LoggerHolder.LOG.warn(exception)
            } else {
                throw exception
            }
        }
    }

    private object LoggerHolder {
        val LOG = Logger.getInstance(LLFirLazyResolveContractChecker::class.java)
    }
}
