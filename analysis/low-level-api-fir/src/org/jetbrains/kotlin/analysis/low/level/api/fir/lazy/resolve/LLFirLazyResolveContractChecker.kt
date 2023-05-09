/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirLazyResolveContractViolationException

internal class LLFirLazyResolveContractChecker {
    private val currentTransformerPhase = ThreadLocal.withInitial<FirResolvePhase?> { null }

    inline fun lazyResolveToPhaseInside(phase: FirResolvePhase, isJumpingPhase: Boolean = false, resolve: () -> Unit) {
        checkIfCanLazyResolveToPhase(phase, isJumpingPhase)
        val previousPhase = currentTransformerPhase.get()
        currentTransformerPhase.set(phase)
        try {
            resolve()
        } finally {
            currentTransformerPhase.set(previousPhase)
        }
    }

    private fun checkIfCanLazyResolveToPhase(requestedPhase: FirResolvePhase, isJumpingPhase: Boolean) {
        val currentPhase = currentTransformerPhase.get() ?: return

        if (requestedPhase > currentPhase || !isJumpingPhase && requestedPhase == currentPhase) {
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
