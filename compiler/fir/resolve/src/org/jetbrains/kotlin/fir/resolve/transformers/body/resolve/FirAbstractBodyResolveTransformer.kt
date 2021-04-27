/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirQualifiedNameResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.FirAbstractPhaseTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.FirSyntheticCallGenerator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) : FirAbstractPhaseTransformer<ResolutionMode>(phase) {
    abstract val context: BodyResolveContext
    abstract val components: BodyResolveTransformerComponents
    abstract val resolutionContext: ResolutionContext

    @set:PrivateForInline
    abstract var implicitTypeOnly: Boolean
        internal set

    override val transformerPhase: FirResolvePhase
        get() = if (implicitTypeOnly) baseTransformerPhase else FirResolvePhase.BODY_RESOLVE

    final override val session: FirSession get() = components.session

    protected open fun needReplacePhase(firDeclaration: FirDeclaration) = true

    fun replaceDeclarationResolvePhaseIfNeeded(firDeclaration: FirDeclaration, newResolvePhase: FirResolvePhase) {
        if (needReplacePhase(firDeclaration) && newResolvePhase > firDeclaration.resolvePhase) {
            firDeclaration.replaceResolvePhase(newResolvePhase)
        }
    }

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
    protected inline val dataFlowAnalyzer: FirDataFlowAnalyzer<*> get() = components.dataFlowAnalyzer
    protected inline val scopeSession: ScopeSession get() = components.scopeSession
    protected inline val file: FirFile get() = components.file

    val ResolutionMode.expectedType: FirTypeRef?
        get() = expectedType(components)

    class BodyResolveTransformerComponents(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        val transformer: FirBodyResolveTransformer,
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

        override val noExpectedType: FirTypeRef = buildImplicitTypeRef()
        override val symbolProvider: FirSymbolProvider = session.symbolProvider

        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner()

        private val qualifiedResolver: FirQualifiedNameResolver = FirQualifiedNameResolver(this)
        override val callResolver: FirCallResolver = FirCallResolver(
            this,
            qualifiedResolver
        )
        val typeResolverTransformer = FirSpecificTypeResolverTransformer(
            session
        )
        override val callCompleter: FirCallCompleter = FirCallCompleter(transformer, this)
        override val dataFlowAnalyzer: FirDataFlowAnalyzer<*> =
            FirDataFlowAnalyzer.createFirDataFlowAnalyzer(this, context.dataFlowAnalyzerContext)
        override val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this)
        override val doubleColonExpressionResolver: FirDoubleColonExpressionResolver = FirDoubleColonExpressionResolver(session)
        override val outerClassManager: FirOuterClassManager = FirOuterClassManager(session, context.outerLocalClassForNested)
        override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession, outerClassManager)
    }
}
