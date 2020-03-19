/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeResolveScopeForBodyResolve
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) : FirAbstractPhaseTransformer<ResolutionMode>(phase) {
    abstract val components: BodyResolveTransformerComponents

    @set:PrivateForInline
    abstract var implicitTypeOnly: Boolean
        internal set

    final override val session: FirSession get() = components.session

    final override val <D> AbstractFirBasedSymbol<D>.phasedFir: D where D : FirDeclaration, D : FirSymbolOwner<D>
        get() {
            val requiredPhase = transformerPhase.requiredToLaunch
            return phasedFir(requiredPhase)
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

    protected inline fun <T> withLocalScopeCleanup(crossinline l: () -> T): T {
        return components.withLocalScopesCleanup(l)
    }

    protected fun addLocalScope(localScope: FirLocalScope?) {
        if (localScope == null) return
        components.addLocalScope(localScope)
    }


    @OptIn(PrivateForInline::class)
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
    protected inline val localScopes: List<FirLocalScope> get() = components.localScopes

    protected inline val noExpectedType: FirTypeRef get() = components.noExpectedType

    protected inline val symbolProvider: FirSymbolProvider get() = components.symbolProvider

    protected inline val implicitReceiverStack: MutableImplicitReceiverStack get() = components.implicitReceiverStack
    protected inline val inferenceComponents: InferenceComponents get() = components.inferenceComponents
    protected inline val resolutionStageRunner: ResolutionStageRunner get() = components.resolutionStageRunner
    protected inline val samResolver: FirSamResolver get() = components.samResolver
    protected inline val typeResolverTransformer: FirSpecificTypeResolverTransformer get() = components.typeResolverTransformer
    protected inline val callResolver: FirCallResolver get() = components.callResolver
    protected inline val callCompleter: FirCallCompleter get() = components.callCompleter
    protected inline val dataFlowAnalyzer: FirDataFlowAnalyzer<*> get() = components.dataFlowAnalyzer
    protected inline val scopeSession: ScopeSession get() = components.scopeSession
    protected inline val file: FirFile get() = components.file
    protected inline val integerLiteralTypeApproximator: IntegerLiteralTypeApproximationTransformer get() = components.integerLiteralTypeApproximator
    protected inline val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater get() = components.integerOperatorsTypeUpdater


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
        override val topLevelScopes: MutableList<FirScope> = mutableListOf()

        @set:PrivateForInline
        override var localScopes: FirLocalScopes = persistentListOf()

        override val noExpectedType: FirTypeRef = buildImplicitTypeRef()

        override val localContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirLocalContext> = mutableMapOf()

        @set:PrivateForInline
        override lateinit var file: FirFile
            internal set

        override val symbolProvider: FirSymbolProvider = session.firSymbolProvider

        override val returnTypeCalculator: ReturnTypeCalculator = transformer.returnTypeCalculator

        @set:PrivateForInline
        override var implicitReceiverStack: MutableImplicitReceiverStack = ImplicitReceiverStackImpl()

        override val inferenceComponents: InferenceComponents = inferenceComponents(session, returnTypeCalculator, scopeSession)
        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner(inferenceComponents)
        override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession)
        override val doubleColonExpressionResolver: FirDoubleColonExpressionResolver = FirDoubleColonExpressionResolver(session)
        private val qualifiedResolver: FirQualifiedNameResolver = FirQualifiedNameResolver(this)
        override val callResolver: FirCallResolver = FirCallResolver(
            this,
            qualifiedResolver
        )
        val typeResolverTransformer = FirSpecificTypeResolverTransformer(
            FirTypeResolveScopeForBodyResolve(this), session
        )
        override val callCompleter: FirCallCompleter = FirCallCompleter(transformer, this)
        override val dataFlowAnalyzer: FirDataFlowAnalyzer<*> = FirDataFlowAnalyzer.createFirDataFlowAnalyzer(this)
        override val syntheticCallGenerator: FirSyntheticCallGenerator = FirSyntheticCallGenerator(this)
        override val integerLiteralTypeApproximator: IntegerLiteralTypeApproximationTransformer =
            IntegerLiteralTypeApproximationTransformer(symbolProvider, inferenceComponents.ctx)
        override val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater = IntegerOperatorsTypeUpdater(integerLiteralTypeApproximator)

        @set:PrivateForInline
        var containerIfAny: FirDeclaration? = null

        override var container: FirDeclaration
            get() = containerIfAny!!
            private set(value) {
                @OptIn(PrivateForInline::class)
                containerIfAny = value
            }

        @OptIn(PrivateForInline::class)
        inline fun <T> withContainer(declaration: FirDeclaration, crossinline f: () -> T): T {
            val prevContainer = containerIfAny
            containerIfAny = declaration
            val result = f()
            containerIfAny = prevContainer
            return result
        }

        @OptIn(PrivateForInline::class)
        inline fun <T> withLocalContext(localContext: FirLocalContext, f: () -> T): T {
            val existedStack = this.implicitReceiverStack
            val existedLocalScopes = this.localScopes

            implicitReceiverStack = localContext.implicitReceiverStack
            localScopes = localContext.localScopes

            return try {
                f()
            } finally {
                implicitReceiverStack = existedStack
                localScopes = existedLocalScopes
            }
        }

        @OptIn(PrivateForInline::class)
        inline fun <R> withLocalScopesCleanup(l: () -> R): R {
            val initialLocalScopes = localScopes
            return try {
                l()
            } finally {
                localScopes = initialLocalScopes
            }
        }

        @OptIn(PrivateForInline::class)
        fun addLocalScope(localScope: FirLocalScope) {
            localScopes = localScopes.add(localScope)
        }

        fun storeClass(klass: FirRegularClass) {
            updateLastScope { storeClass(klass) }
        }

        fun storeFunction(function: FirSimpleFunction) {
            updateLastScope { storeFunction(function) }
        }

        fun storeVariable(variable: FirVariable<*>) {
            updateLastScope { storeVariable(variable) }
        }

        override fun saveContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            localContextForAnonymousFunctions[anonymousFunction.symbol] = FirLocalContext(localScopes, implicitReceiverStack.snapshot())
        }

        override fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            localContextForAnonymousFunctions.remove(anonymousFunction.symbol)
        }

        fun cleanContextForAnonymousFunction() {
            localContextForAnonymousFunctions.clear()
        }

        @OptIn(PrivateForInline::class)
        private inline fun updateLastScope(transform: FirLocalScope.() -> FirLocalScope) {
            val lastScope = localScopes.lastOrNull() ?: return
            localScopes = localScopes.set(localScopes.size - 1, lastScope.transform())
        }
    }
}

