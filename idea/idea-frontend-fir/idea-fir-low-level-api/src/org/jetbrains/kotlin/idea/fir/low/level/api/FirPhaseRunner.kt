/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.createTransformerBasedProcessorByPhase
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FirPhaseRunner {
    private val superTypesBodyResolveLock = ReentrantLock()
    private val implicitTypesResolveLock = ReentrantLock()

    fun runPhase(firFile: FirFile, phase: FirResolvePhase, scopeSession: ScopeSession) = when (phase) {
        FirResolvePhase.SUPER_TYPES -> superTypesBodyResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
        else -> {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
    }

    private fun runPhaseWithoutLock(firFile: FirFile, phase: FirResolvePhase, scopeSession: ScopeSession) {
        val phaseProcessor = phase.createTransformerBasedProcessorByPhase(firFile.session, scopeSession)
        executeWithoutPCE { phaseProcessor.processFile(firFile) }
    }
}