/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.*
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

fun FirResolvePhase.createCompilerProcessorByPhase(
    session: FirSession,
    scopeSession: ScopeSession
): FirResolveProcessor {
    return when (this) {
        RAW_FIR -> throw IllegalStateException("Raw FIR building phase does not have a transformer")
        ANNOTATIONS_FOR_PLUGINS -> FirPluginAnnotationsResolveProcessor(session, scopeSession)
        CLASS_GENERATION -> FirGlobalClassGenerationProcessor(session, scopeSession)
        IMPORTS -> FirImportResolveProcessor(session, scopeSession)
        SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsProcessor(session, scopeSession)
        TYPES -> FirTypeResolveProcessor(session, scopeSession)
        EXTENSION_STATUS_UPDATE -> FirGlobalExtensionStatusProcessor(session, scopeSession)
        STATUS -> FirStatusResolveProcessor(session, scopeSession)
        CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
        NEW_MEMBERS_GENERATION -> FirGlobalNewMemberGenerationProcessor(session, scopeSession)
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
        BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession)
    }
}

fun FirResolvePhase.createTransformerBasedProcessorByPhase(
    session: FirSession,
    scopeSession: ScopeSession
): FirTransformerBasedResolveProcessor {
    return when (this) {
        RAW_FIR -> throw IllegalStateException("Raw FIR building phase does not have a transformer")
        ANNOTATIONS_FOR_PLUGINS -> FirPluginAnnotationsResolveProcessor(session, scopeSession)
        CLASS_GENERATION -> FirDummyTransformerBasedProcessor(session, scopeSession) // TODO: remove
        IMPORTS -> FirImportResolveProcessor(session, scopeSession)
        SUPER_TYPES -> FirSupertypeResolverProcessor(session, scopeSession)
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsProcessor(session, scopeSession)
        TYPES -> FirTypeResolveProcessor(session, scopeSession)
        EXTENSION_STATUS_UPDATE -> FirTransformerBasedExtensionStatusProcessor(session, scopeSession)
        STATUS -> FirStatusResolveProcessor(session, scopeSession)
        CONTRACTS -> FirContractResolveProcessor(session, scopeSession)
        NEW_MEMBERS_GENERATION -> FirDummyTransformerBasedProcessor(session, scopeSession) // TODO: remove
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveProcessor(session, scopeSession)
        BODY_RESOLVE -> FirBodyResolveProcessor(session, scopeSession)
    }
}

private class FirDummyTransformerBasedProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer: FirTransformer<Nothing?>
        get() = DummyTransformer

    private object DummyTransformer : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }
    }
}