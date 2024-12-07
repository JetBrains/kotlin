/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.ImplicitValueStorage
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.SmartcastStability

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
    expandTypeAliases = true,
) {
    override val dataFlowAnalyzer: FirDataFlowAnalyzer
        get() = object : FirDataFlowAnalyzer(this@StubBodyResolveTransformerComponents, context.dataFlowAnalyzerContext) {
            override val logicSystem: LogicSystem
                get() = error("Should not be called")

            override val receiverStack: ImplicitValueStorage
                get() = error("Should not be called")

            override fun implicitUpdated(info: TypeStatement) =
                error("Should not be called")

            override fun getTypeUsingSmartcastInfo(expression: FirExpression): Pair<SmartcastStability, Set<ConeKotlinType>>? =
                null
        }
}