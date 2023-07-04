/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImplWithoutSource
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) : FirAbstractPhaseTransformer<ResolutionMode>(phase) {
    abstract val context: BodyResolveContext
    abstract val components: BodyResolveTransformerComponents
    abstract val resolutionContext: ResolutionContext

    @set:PrivateForInline
    abstract var implicitTypeOnly: Boolean
        internal set

    final override val session: FirSession get() = components.session

    @OptIn(PrivateForInline::class)
    internal inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        val shouldSwitchMode = implicitTypeOnly
        if (shouldSwitchMode) {
            implicitTypeOnly = false
        }
        return try {
            l()
        } finally {
            if (shouldSwitchMode) {
                implicitTypeOnly = true
            }
        }
    }

    override fun transformLazyExpression(lazyExpression: FirLazyExpression, data: ResolutionMode): FirStatement {
        suppressOrThrowError("FirLazyExpression should be calculated before accessing", lazyExpression)
        return lazyExpression
    }

    override fun transformLazyBlock(lazyBlock: FirLazyBlock, data: ResolutionMode): FirStatement {
        suppressOrThrowError("FirLazyBlock should be calculated before accessing", lazyBlock)
        return lazyBlock
    }

    private fun suppressOrThrowError(message: String, element: FirElement) {
        if (System.getProperty("kotlin.suppress.lazy.expression.access").toBoolean()) return
        errorWithAttachment(message) {
            withFirEntry("firElement", element)
        }
    }

    protected inline val localScopes: List<FirLocalScope> get() = components.localScopes

    protected inline val noExpectedType: FirTypeRef get() = components.noExpectedType

    protected inline val symbolProvider: FirSymbolProvider get() = components.symbolProvider

    protected inline val implicitReceiverStack: ImplicitReceiverStack get() = components.implicitReceiverStack
    protected inline val inferenceComponents: InferenceComponents get() = session.inferenceComponents
    protected inline val resolutionStageRunner: ResolutionStageRunner get() = components.resolutionStageRunner
    protected inline val samResolver: FirSamResolver get() = components.samResolver
    protected inline val typeResolverTransformer: FirSpecificTypeResolverTransformer get() = components.typeResolverTransformer
    protected inline val callResolver: FirCallResolver get() = components.callResolver
    protected inline val callCompleter: FirCallCompleter get() = components.callCompleter
    inline val dataFlowAnalyzer: FirDataFlowAnalyzer get() = components.dataFlowAnalyzer
    protected inline val scopeSession: ScopeSession get() = components.scopeSession
    protected inline val file: FirFile get() = components.file

    val ResolutionMode.expectedType: FirTypeRef?
        get() = expectedType(components)

    open class BodyResolveTransformerComponents(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        val transformer: FirAbstractBodyResolveTransformerDispatcher,
        val context: BodyResolveContext
    ) : BodyResolveComponents() {
        override val fileImportsScope: List<FirScope> get() = context.fileImportsScope
        override val towerDataElements: List<FirTowerDataElement> get() = context.towerDataContext.towerDataElements
        override val localScopes: FirLocalScopes get() = context.towerDataContext.localScopes

        override val towerDataContext: FirTowerDataContext get() = context.towerDataContext

        override val file: FirFile get() = context.file
        override val implicitReceiverStack: ImplicitReceiverStack get() = context.implicitReceiverStack
        override val containingDeclarations: List<FirDeclaration> get() = context.containers
        override val returnTypeCalculator: ReturnTypeCalculator get() = context.returnTypeCalculator
        override val container: FirDeclaration get() = context.containerIfAny!!

        override val noExpectedType: FirTypeRef = FirImplicitTypeRefImplWithoutSource
        override val symbolProvider: FirSymbolProvider = session.symbolProvider

        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner()

        override val callResolver: FirCallResolver = FirCallResolver(
            this,
        )
        val typeResolverTransformer = FirSpecificTypeResolverTransformer(
            session
        )
        override val callCompleter: FirCallCompleter = FirCallCompleter(transformer, this)
        override val dataFlowAnalyzer: FirDataFlowAnalyzer =
            FirDataFlowAnalyzer.createFirDataFlowAnalyzer(this, context.dataFlowAnalyzerContext)
        override val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this)
        override val doubleColonExpressionResolver: FirDoubleColonExpressionResolver = FirDoubleColonExpressionResolver(session)
        override val outerClassManager: FirOuterClassManager = FirOuterClassManager(session, context.outerLocalClassForNested)
        override val samResolver: FirSamResolver = FirSamResolver(session, scopeSession, outerClassManager)
        override val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer =
            IntegerLiteralAndOperatorApproximationTransformer(session, scopeSession)
    }
}
