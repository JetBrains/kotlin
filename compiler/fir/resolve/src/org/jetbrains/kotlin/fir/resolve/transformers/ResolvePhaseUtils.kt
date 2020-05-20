/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirGlobalExtensionStatusProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirPluginAnnotationsResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirTransformerBasedExtensionStatusProcessor

fun FirResolvePhase.createProcessorByPhase(
    session: FirSession,
    scopeSession: ScopeSession,
    compilerMode: CompilerMode = CompilerMode.CLI
): FirResolveProcessor {
    return when (this) {
        RAW_FIR -> throw IllegalStateException("Raw FIR building phase does not have a transformer")
        ANNOTATIONS_FOR_PLUGINS -> FirPluginAnnotationsResolveProcessor(session, scopeSession)
//        FIRST_PLUGIN_GENERATION -> FirFirstGenerationTransformer()
        IMPORTS -> FirImportResolveProcessor(session, scopeSession)
        SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsProcessor(session, scopeSession)
        TYPES -> FirTypeResolveProcessor(session, scopeSession)
        EXTENSION_STATUS_UPDATE -> when (compilerMode) {
            CompilerMode.CLI -> FirGlobalExtensionStatusProcessor(session, scopeSession)
            CompilerMode.IDE -> FirTransformerBasedExtensionStatusProcessor(session, scopeSession)
        }
        STATUS -> FirStatusResolveProcessor(session, scopeSession)
        CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
        BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession)
    }
}

fun FirResolvePhase.createTransformerBasedProcessorByPhase(session: FirSession, scopeSession: ScopeSession): FirTransformerBasedResolveProcessor {
    return createProcessorByPhase(session, scopeSession, CompilerMode.IDE) as FirTransformerBasedResolveProcessor
}

enum class CompilerMode {
    CLI, IDE
}