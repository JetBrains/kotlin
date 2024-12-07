/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve

open class FirBodyResolveTransformer(
    session: FirSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    scopeSession: ScopeSession,
    returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
    outerBodyResolveContext: BodyResolveContext? = null,
) : FirAbstractBodyResolveTransformerDispatcher(
    session,
    phase,
    implicitTypeOnly,
    scopeSession,
    returnTypeCalculator,
    outerBodyResolveContext,
    expandTypeAliases = true,
) {
    final override val expressionsTransformer: FirExpressionsResolveTransformer = FirExpressionsResolveTransformer(this)
    final override val declarationsTransformer: FirDeclarationsResolveTransformer = FirDeclarationsResolveTransformer(this)
}
