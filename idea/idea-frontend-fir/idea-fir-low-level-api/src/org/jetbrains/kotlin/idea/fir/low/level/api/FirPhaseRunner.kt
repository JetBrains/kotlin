/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirPluginAnnotationsResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirTransformerBasedExtensionStatusProcessor
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyBodiesCalculator
import org.jetbrains.kotlin.idea.fir.low.level.api.util.executeWithoutPCE
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FirPhaseRunner {
    private val superTypesBodyResolveLock = ReentrantLock()
    private val statusResolveLock = ReentrantLock()
    private val implicitTypesResolveLock = ReentrantLock()

    fun runPhase(firFile: FirFile, phase: FirResolvePhase, scopeSession: ScopeSession) = when (phase) {
        FirResolvePhase.SUPER_TYPES -> superTypesBodyResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
        FirResolvePhase.STATUS -> statusResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> implicitTypesResolveLock.withLock {
            runPhaseWithoutLock(firFile, phase, scopeSession)
        }
        //TODO: Make correct sync for TYPES
        else -> {
            runPhaseWithoutLock(firFile, phase, scopeSession)
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
        //TODO: Make correct sync for TYPES
        else -> {
            runPhaseWithCustomResolveWithoutLock(resolve)
        }
    }

    private inline fun runPhaseWithCustomResolveWithoutLock(crossinline resolve: () -> Unit) {
        executeWithoutPCE {
            resolve()
        }
    }

    private fun runPhaseWithoutLock(firFile: FirFile, phase: FirResolvePhase, scopeSession: ScopeSession) {
        val phaseProcessor = phase.createTransformerBasedProcessorByPhase(firFile.moduleData.session, scopeSession)
        executeWithoutPCE {
            FirLazyBodiesCalculator.calculateLazyBodiesIfPhaseRequires(firFile, phase)
            phaseProcessor.processFile(firFile)
        }
    }
}

internal fun FirResolvePhase.createTransformerBasedProcessorByPhase(
    session: FirSession,
    scopeSession: ScopeSession
): FirTransformerBasedResolveProcessor {
    return when (this) {
        FirResolvePhase.RAW_FIR -> throw IllegalStateException("Raw FIR building phase does not have a transformer")
        FirResolvePhase.ANNOTATIONS_FOR_PLUGINS -> FirPluginAnnotationsResolveProcessor(session, scopeSession)
        FirResolvePhase.CLASS_GENERATION -> FirDummyTransformerBasedProcessor(session, scopeSession) // TODO: remove
        FirResolvePhase.IMPORTS -> FirImportResolveProcessor(session, scopeSession)
        FirResolvePhase.SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
        FirResolvePhase.SEALED_CLASS_INHERITORS -> FirDummyTransformerBasedProcessor(session, scopeSession)
        FirResolvePhase.TYPES -> FirTypeResolveProcessor(session, scopeSession)
        FirResolvePhase.ARGUMENTS_OF_PLUGIN_ANNOTATIONS -> FirAnnotationArgumentsResolveProcessor(session, scopeSession)
        FirResolvePhase.EXTENSION_STATUS_UPDATE -> FirTransformerBasedExtensionStatusProcessor(session, scopeSession)
        FirResolvePhase.STATUS -> FirStatusResolveProcessor(session, scopeSession)
        FirResolvePhase.CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
        FirResolvePhase.NEW_MEMBERS_GENERATION -> FirDummyTransformerBasedProcessor(session, scopeSession) // TODO: remove
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
        FirResolvePhase.BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession)
    }
}
