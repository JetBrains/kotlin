/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

internal fun createStubBodyResolveComponents(firSession: FirSession): FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents {
    val scopeSession = ScopeSession()

    // This transformer is not intended for actual transformations and created here only to simplify access to resolve components
    val stubBodyResolveTransformer = FirBodyResolveTransformer(
        session = firSession,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    )

    return StubBodyResolveTransformerComponents(
        firSession,
        scopeSession,
        stubBodyResolveTransformer,
        stubBodyResolveTransformer.context,
    )
}

internal open class StubBodyResolveTransformerComponents(
    session: FirSession,
    scopeSession: ScopeSession,
    transformer: FirBodyResolveTransformer,
    context: BodyResolveContext
) : FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
    session,
    scopeSession,
    transformer,
    context,
) {
    override val dataFlowAnalyzer: FirDataFlowAnalyzer
        get() = object : FirDataFlowAnalyzer(this@StubBodyResolveTransformerComponents, context.dataFlowAnalyzerContext) {
            override val logicSystem: LogicSystem
                get() = error("Should not be called")

            override val receiverStack: Iterable<ImplicitReceiverValue<*>>
                get() = error("Should not be called")

            override fun receiverUpdated(symbol: FirBasedSymbol<*>, info: TypeStatement?) =
                error("Should not be called")

            override fun getTypeUsingSmartcastInfo(
                expression: FirExpression,
                ignoreCallArguments: Boolean,
            ): Pair<PropertyStability, MutableList<ConeKotlinType>>? =
                null
        }
}