/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer

internal fun createStubBodyResolveComponents(firSession: FirSession): FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents {
    val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    val stubBodyResolveTransformer = FirBodyResolveTransformer(
        session = firSession,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    return FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    )
}