/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformerAdapter
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveTransformerAdapter
import org.jetbrains.kotlin.fir.visitors.FirTransformer

// TODO: add FirSession parameter
fun FirResolvePhase.createTransformerByPhase(): FirTransformer<Nothing?> {
    return when (this) {
        RAW_FIR -> throw AssertionError("Raw FIR building phase does not have a transformer")
        IMPORTS -> FirImportResolveTransformer()
        SUPER_TYPES -> FirSupertypeResolverTransformer()
        SEALED_CLASS_INHERITORS -> FirSealedClassInheritorsTransformer()
        TYPES -> FirTypeResolveTransformer()
        STATUS -> FirStatusResolveTransformer()
        IMPLICIT_TYPES_BODY_RESOLVE -> FirImplicitTypeBodyResolveTransformerAdapter()
        BODY_RESOLVE -> FirBodyResolveTransformerAdapter()
    }
}
