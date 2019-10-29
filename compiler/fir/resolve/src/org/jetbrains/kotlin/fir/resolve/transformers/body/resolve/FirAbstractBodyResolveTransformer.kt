/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirCallResolver
import org.jetbrains.kotlin.fir.FirQualifiedNameResolver
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeResolveScopeForBodyResolve
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitTypeRefImpl

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) : FirAbstractPhaseTransformer<ResolutionMode>(phase) {
    abstract val components: BodyResolveTransformerComponents

    abstract var implicitTypeOnly: Boolean
        internal set

    final override val session: FirSession get() = components.session

    final override val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.prev
            return phasedFir(session, requiredPhase)
        }

    protected inline fun <T> withScopeCleanup(scopes: MutableList<*>, crossinline l: () -> T): T {
        val sizeBefore = scopes.size
        return try {
            l()
        } finally {
            val size = scopes.size
            assert(size >= sizeBefore)
            repeat(size - sizeBefore) {
                scopes.let { it.removeAt(it.size - 1) }
            }
        }
    }

    internal inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        if (!implicitTypeOnly) return l()
        implicitTypeOnly = false
        return try {
            l()
        } finally {
            implicitTypeOnly = true
        }
    }

    protected inline val topLevelScopes: MutableList<FirScope> get() = components.topLevelScopes
    protected inline val localScopes: MutableList<FirLocalScope> get() = components.localScopes

    protected inline val noExpectedType: FirTypeRef get() = components.noExpectedType

    protected inline val symbolProvider: FirSymbolProvider get() = components.symbolProvider

    protected inline val returnTypeCalculator: ReturnTypeCalculator get() = components.returnTypeCalculator
    protected inline val implicitReceiverStack: ImplicitReceiverStack get() = components.implicitReceiverStack
    protected inline val inferenceComponents: InferenceComponents get() = components.inferenceComponents
    protected inline val resolutionStageRunner: ResolutionStageRunner get() = components.resolutionStageRunner
    protected inline val samResolver: FirSamResolver get() = components.samResolver
    protected inline val typeResolverTransformer: FirSpecificTypeResolverTransformer get() = components.typeResolverTransformer
    protected inline val callCompleter: FirCallCompleter get() = components.callCompleter
    protected inline val dataFlowAnalyzer: FirDataFlowAnalyzer get() = components.dataFlowAnalyzer
    protected inline val scopeSession: ScopeSession get() = components.scopeSession
    protected inline val file: FirFile get() = components.file

    val ResolutionMode.expectedType: FirTypeRef?
        get() = when (this) {
            is ResolutionMode.WithExpectedType -> expectedTypeRef
            is ResolutionMode.ContextIndependent -> noExpectedType
            else -> null
        }

    class BodyResolveTransformerComponents(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        val transformer: FirBodyResolveTransformer
    ) : BodyResolveComponents {
        val topLevelScopes: MutableList<FirScope> = mutableListOf()
        val localScopes: MutableList<FirLocalScope> = mutableListOf()

        override val noExpectedType: FirTypeRef = FirImplicitTypeRefImpl(null)

        override lateinit var file: FirFile
            internal set

        override val symbolProvider: FirSymbolProvider = session.firSymbolProvider

        override val returnTypeCalculator: ReturnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession)
        override val implicitReceiverStack: ImplicitReceiverStack = ImplicitReceiverStackImpl()
        override val inferenceComponents: InferenceComponents = inferenceComponents(session, returnTypeCalculator, scopeSession)
        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner(inferenceComponents)
        override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession)
        override val doubleColonExpressionResolver: FirDoubleColonExpressionResolver = FirDoubleColonExpressionResolver(session)
        private val qualifiedResolver: FirQualifiedNameResolver = FirQualifiedNameResolver(this)
        override val callResolver: FirCallResolver = FirCallResolver(
            this,
            topLevelScopes,
            localScopes,
            implicitReceiverStack,
            qualifiedResolver
        )
        val typeResolverTransformer = FirSpecificTypeResolverTransformer(
            FirTypeResolveScopeForBodyResolve(topLevelScopes, localScopes), session
        )
        val callCompleter: FirCallCompleter = FirCallCompleter(transformer, this)
        val dataFlowAnalyzer: FirDataFlowAnalyzer = FirDataFlowAnalyzer(this)
        override val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this, callCompleter)

        internal var containerIfAny: FirDeclaration? = null
            private set

        override var container: FirDeclaration
            get() = containerIfAny!!
            private set(value) {
                containerIfAny = value
            }

        internal inline fun <T> withContainer(declaration: FirDeclaration, crossinline f: () -> T): T {
            val prevContainer = containerIfAny
            containerIfAny = declaration
            val result = f()
            containerIfAny = prevContainer
            return result
        }
    }
}

