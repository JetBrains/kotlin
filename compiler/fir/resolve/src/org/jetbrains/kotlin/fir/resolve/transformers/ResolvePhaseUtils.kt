/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformerAdapter
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveTransformerAdapter
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveTransformerAdapter
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirFirstGenerationTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirPluginAnnotationsResolveTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer

// TODO: add FirSession parameter
@OptIn(AdapterForResolvePhase::class)
fun FirResolvePhase.createTransformerByPhase(scopeSession: ScopeSession): FirTransformer<Nothing?> {
    return when (this) {
        RAW_FIR -> throw AssertionError("Raw FIR building phase does not have a transformer")
        ANNOTATIONS_FOR_PLUGINS -> FirPluginAnnotationsResolveTransformer(scopeSession)
        FIRST_PLUGIN_GENERATION -> FirFirstGenerationTransformer()
        IMPORTS -> FirImportResolveTransformer()
        SUPER_TYPES -> FirSupertypeResolverTransformer(scopeSession)
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsTransformer()
        TYPES -> FirTypeResolveTransformerAdapter(scopeSession)
        STATUS -> FirStatusResolveTransformerAdapter()
        CONTRACTS -> FirContractResolveTransformerAdapter(scopeSession)
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveTransformerAdapter(scopeSession)
        BODY_RESOLVE -> FirBodyResolveTransformerAdapter(scopeSession)
    }
}

@RequiresOptIn(message = "Should be used just once from createTransformerByPhase")
annotation class AdapterForResolvePhase
