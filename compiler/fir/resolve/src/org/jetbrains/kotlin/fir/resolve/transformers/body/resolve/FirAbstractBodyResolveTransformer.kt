/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.FirDataFlowAnalyzer
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildImplicitTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.sure

abstract class FirAbstractBodyResolveTransformer(phase: FirResolvePhase) : FirAbstractPhaseTransformer<ResolutionMode>(phase) {
    abstract val context: BodyResolveContext
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

    protected inline fun <T> withLocalScopeCleanup(crossinline l: () -> T): T {
        return context.withTowerDataCleanup(l)
    }

    protected inline fun <T> withNewLocalScope(crossinline l: () -> T): T {
        return context.withTowerDataCleanup {
            addNewLocalScope()
            l()
        }
    }

    protected fun addNewLocalScope() {
        context.addLocalScope(FirLocalScope())
    }

    protected fun addLocalScope(localScope: FirLocalScope?) {
        if (localScope == null) return
        context.addLocalScope(localScope)
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

    protected inline val localScopes: List<FirLocalScope> get() = components.localScopes

    protected inline val noExpectedType: FirTypeRef get() = components.noExpectedType

    protected inline val symbolProvider: FirSymbolProvider get() = components.symbolProvider

    protected inline val implicitReceiverStack: ImplicitReceiverStack get() = components.implicitReceiverStack
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

    class BodyResolveContext(
        val returnTypeCalculator: ReturnTypeCalculator,
        val dataFlowAnalyzerContext: DataFlowAnalyzerContext<PersistentFlow>,
        val targetedLocalClasses: Set<FirClass<*>> = emptySet(),
        val outerLocalClassForNested: MutableMap<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>> = mutableMapOf()
    ) {
        val fileImportsScope: MutableList<FirScope> = mutableListOf()

        @set:PrivateForInline
        lateinit var file: FirFile
            internal set

        val implicitReceiverStack: ImplicitReceiverStack get() = towerDataContext.implicitReceiverStack

        @set:PrivateForInline
        var towerDataContext: FirTowerDataContext = FirTowerDataContext()

        @set:PrivateForInline
        var towerDataContextsForClassParts: FirTowerDataContextsForClassParts? = null

        val containerIfAny: FirDeclaration?
            get() = containers.lastOrNull()

        @set:PrivateForInline
        var containers: PersistentList<FirDeclaration> = persistentListOf()

        val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext> = mutableMapOf()

        @OptIn(PrivateForInline::class)
        inline fun <T> withNewTowerDataForClassParts(newContexts: FirTowerDataContextsForClassParts, f: () -> T): T {
            val old = towerDataContextsForClassParts

            towerDataContextsForClassParts = newContexts

            return try {
                f()
            } finally {

                towerDataContextsForClassParts = old
            }
        }

        fun getTowerDataContextForStaticNestedClassesUnsafe(): FirTowerDataContext =
            firTowerDataContextsForClassParts().forNestedClasses

        fun getTowerDataContextForConstructorResolution(): FirTowerDataContext =
            firTowerDataContextsForClassParts().forConstructorHeaders

        fun getPrimaryConstructorParametersScope(): FirLocalScope? =
            towerDataContextsForClassParts?.primaryConstructorParametersScope

        private fun firTowerDataContextsForClassParts() =
            towerDataContextsForClassParts.sure { "towerDataContextForStaticNestedClasses should not be null" }

        @OptIn(PrivateForInline::class)
        inline fun <T> withContainer(declaration: FirDeclaration, crossinline f: () -> T): T {
            val oldContainers = containers
            containers = containers.add(declaration)
            return try {
                f()
            } finally {
                containers = oldContainers
            }
        }

        inline fun <T> withTowerDataContext(context: FirTowerDataContext, f: () -> T): T {
            return withTowerDataCleanup {
                replaceTowerDataContext(context)
                f()
            }
        }

        @OptIn(PrivateForInline::class)
        inline fun <R> withTowerDataCleanup(l: () -> R): R {
            val initialContext = towerDataContext
            return try {
                l()
            } finally {
                towerDataContext = initialContext
            }
        }

        @OptIn(PrivateForInline::class)
        fun replaceTowerDataContext(newContext: FirTowerDataContext) {
            towerDataContext = newContext
        }

        fun addNonLocalTowerDataElement(element: FirTowerDataElement) {
            replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(listOf(element)))
        }

        fun addNonLocalTowerDataElements(newElements: List<FirTowerDataElement>) {
            replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(newElements))
        }

        fun addLocalScope(localScope: FirLocalScope) {
            replaceTowerDataContext(towerDataContext.addLocalScope(localScope))
        }

        fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>) {
            replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue))
        }

        fun storeClassIfNotNested(klass: FirRegularClass) {
            if (containerIfAny is FirClass<*>) return
            updateLastScope { storeClass(klass) }
        }

        fun storeFunction(function: FirSimpleFunction) {
            updateLastScope { storeFunction(function) }
        }

        fun storeVariable(variable: FirVariable<*>) {
            updateLastScope { storeVariable(variable) }
        }

        fun saveContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            towerDataContextForAnonymousFunctions[anonymousFunction.symbol] = towerDataContext
        }

        fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            towerDataContextForAnonymousFunctions.remove(anonymousFunction.symbol)
        }

        fun cleanContextForAnonymousFunction() {
            towerDataContextForAnonymousFunctions.clear()
        }

        fun cleanDataFlowContext() {
            dataFlowAnalyzerContext.reset()
        }

        private inline fun updateLastScope(transform: FirLocalScope.() -> FirLocalScope) {
            val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
            replaceTowerDataContext(towerDataContext.setLastLocalScope(lastScope.transform()))
        }

        @OptIn(PrivateForInline::class)
        fun createSnapshotForLocalClasses(
            returnTypeCalculator: ReturnTypeCalculator,
            targetedLocalClasses: Set<FirClass<*>>
        ) = BodyResolveContext(returnTypeCalculator, dataFlowAnalyzerContext, targetedLocalClasses, outerLocalClassForNested).apply {
            file = this@BodyResolveContext.file
            towerDataContextForAnonymousFunctions.putAll(this@BodyResolveContext.towerDataContextForAnonymousFunctions)
            containers = this@BodyResolveContext.containers
            towerDataContext = this@BodyResolveContext.towerDataContext
        }
    }

    class BodyResolveTransformerComponents(
        override val session: FirSession,
        override val scopeSession: ScopeSession,
        val transformer: FirBodyResolveTransformer,
        val context: BodyResolveContext
    ) : BodyResolveComponents {
        override val fileImportsScope: List<FirScope> get() = context.fileImportsScope
        override val towerDataElements: List<FirTowerDataElement> get() = context.towerDataContext.towerDataElements
        override val localScopes: FirLocalScopes get() = context.towerDataContext.localScopes

        override val towerDataContext: FirTowerDataContext get() = context.towerDataContext

        override val file: FirFile get() = context.file
        override val implicitReceiverStack: ImplicitReceiverStack get() = context.implicitReceiverStack
        override val containingDeclarations: List<FirDeclaration> get() = context.containers
        override val towerDataContextForAnonymousFunctions: TowerDataContextForAnonymousFunctions get() = context.towerDataContextForAnonymousFunctions
        override val returnTypeCalculator: ReturnTypeCalculator get() = context.returnTypeCalculator
        override val container: FirDeclaration get() = context.containerIfAny!!

        override val noExpectedType: FirTypeRef = buildImplicitTypeRef()
        override val symbolProvider: FirSymbolProvider = session.firSymbolProvider

        override val inferenceComponents: InferenceComponents = inferenceComponents(session, returnTypeCalculator, scopeSession)
        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner(inferenceComponents)
        override val samResolver: FirSamResolver = FirSamResolverImpl(session, scopeSession)
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
        override val integerLiteralTypeApproximator: IntegerLiteralTypeApproximationTransformer =
            IntegerLiteralTypeApproximationTransformer(symbolProvider, inferenceComponents.ctx, inferenceComponents.session)
        override val doubleColonExpressionResolver: FirDoubleColonExpressionResolver =
            FirDoubleColonExpressionResolver(session, integerLiteralTypeApproximator)
        override val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater = IntegerOperatorsTypeUpdater(integerLiteralTypeApproximator)
        override val outerClassManager: FirOuterClassManager = FirOuterClassManager(session, context.outerLocalClassForNested)
    }
}
