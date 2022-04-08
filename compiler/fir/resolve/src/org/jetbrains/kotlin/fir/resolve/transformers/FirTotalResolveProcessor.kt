/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.*

class FirTotalResolveProcessor(session: FirSession) {
    val scopeSession: ScopeSession = ScopeSession()

    private val processors: List<FirResolveProcessor> = createAllCompilerResolveProcessors(
        session,
        scopeSession
    )

    fun process(files: List<FirFile>) {
        for (processor in processors) {
            processor.beforePhase()
            when (processor) {
                is FirTransformerBasedResolveProcessor -> {
                    for (file in files) {
                        processor.processFile(file)
                    }
                }
                is FirGlobalResolveProcessor -> {
                    processor.process(files)
                }
            }
            processor.afterPhase()
        }
    }
}

fun createAllCompilerResolveProcessors(
    session: FirSession,
    scopeSession: ScopeSession? = null
): List<FirResolveProcessor> {
    return createAllResolveProcessors(scopeSession) {
        createCompilerProcessorByPhase(session, it)
    }
}

private inline fun <T : FirResolveProcessor> createAllResolveProcessors(
    scopeSession: ScopeSession? = null,
    creator: FirResolvePhase.(ScopeSession) -> T
): List<T> {
    @Suppress("NAME_SHADOWING")
    val scopeSession = scopeSession ?: ScopeSession()
    val phases = FirResolvePhase.values().filter {
        !it.noProcessor
    }
    return phases.map { it.creator(scopeSession) }
}

fun FirResolvePhase.createCompilerProcessorByPhase(
    session: FirSession,
    scopeSession: ScopeSession
): FirResolveProcessor {
    return when (this) {
        RAW_FIR -> throw IllegalArgumentException("Raw FIR building phase does not have a transformer")
        COMPILER_REQUIRED_ANNOTATIONS -> FirCompilerRequiredAnnotationsResolveProcessor(session, scopeSession)
        COMPANION_GENERATION -> FirCompanionGenerationProcessor(session, scopeSession)
        IMPORTS -> FirImportResolveProcessor(session, scopeSession)
        SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsProcessor(session, scopeSession)
        TYPES -> FirTypeResolveProcessor(session, scopeSession)
        STATUS -> FirStatusResolveProcessor(session, scopeSession)
        ARGUMENTS_OF_ANNOTATIONS -> FirAnnotationArgumentsResolveProcessor(session, scopeSession)
        CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
        BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession)
        EXPECT_ACTUAL_MATCHING -> FirExpectActualMatcherProcessor(session, scopeSession)
    }
}
