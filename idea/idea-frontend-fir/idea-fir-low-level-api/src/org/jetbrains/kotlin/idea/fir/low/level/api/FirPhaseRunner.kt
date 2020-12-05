/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FirPhaseRunner(val transformerProvider: FirTransformerProvider) {
    private val superTypesBodyResolveLock = ReentrantLock()
    private val statusResolveLock = ReentrantLock()
    private val implicitTypesResolveLock = ReentrantLock()

    fun runPhase(firFile: FirFile, phase: FirResolvePhase) = when (phase) {
        FirResolvePhase.SUPER_TYPES -> superTypesBodyResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase)
        }
        FirResolvePhase.STATUS -> statusResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase)
        }
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase)
        }
        else -> {
            runPhaseWithoutLock(firFile, phase)
        }
    }

    inline fun runPhaseWithCustomResolve(phase: FirResolvePhase, crossinline resolve: () -> Unit) = when (phase) {
        FirResolvePhase.SUPER_TYPES -> superTypesBodyResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        FirResolvePhase.STATUS -> statusResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesResolveLock.withLock {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
        else -> {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
    }

    private inline fun runPhaseWithCustomResolveWithoutLock(crossinline resolve: () -> Unit) {
        executeWithoutPCE {
            resolve()
        }
    }


    private fun runPhaseWithoutLock(firFile: FirFile, phase: FirResolvePhase) {
        val phaseProcessor = transformerProvider.getTransformerForPhase(firFile.session, phase)
        executeWithoutPCE {
            FirLazyBodiesCalculator.calculateLazyBodiesIfPhaseRequires(firFile, phase)
            phaseProcessor.processFile(firFile)
        }
    }
}
