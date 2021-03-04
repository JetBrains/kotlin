/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.withScopeCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.Name

class BodyResolveContext(
    val returnTypeCalculator: ReturnTypeCalculator,
    val dataFlowAnalyzerContext: DataFlowAnalyzerContext<PersistentFlow>,
    val targetedLocalClasses: Set<FirClass<*>> = emptySet(),
    val outerLocalClassForNested: MutableMap<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>> = mutableMapOf()
) {
    private val mutableFileImportsScope: MutableList<FirScope> = mutableListOf()

    val fileImportsScope: List<FirScope>
        get() = mutableFileImportsScope

    @set:PrivateForInline
    lateinit var file: FirFile
        private set

    @set:PrivateForInline
    var towerDataContextsForClassParts: FirTowerDataContextsForClassParts =
        FirTowerDataContextsForClassParts(forMemberDeclarations = FirTowerDataContext())

    val towerDataContext: FirTowerDataContext
        get() = towerDataContextsForClassParts.currentContext

    val implicitReceiverStack: ImplicitReceiverStack
        get() = towerDataContext.implicitReceiverStack

    val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext>
        get() = towerDataContextsForClassParts.towerDataContextForAnonymousFunctions

    val towerDataContextForCallableReferences: MutableMap<FirCallableReferenceAccess, FirTowerDataContext>
        get() = towerDataContextsForClassParts.towerDataContextForCallableReferences

    @set:PrivateForInline
    var containers: PersistentList<FirDeclaration> = persistentListOf()

    val containerIfAny: FirDeclaration?
        get() = containers.lastOrNull()

    @set:PrivateForInline
    var inferenceSession: FirInferenceSession = FirInferenceSession.DEFAULT

    val anonymousFunctionsAnalyzedInDependentContext: MutableSet<FirFunctionSymbol<*>> = mutableSetOf()

    @OptIn(PrivateForInline::class)
    inline fun <R> withInferenceSession(inferenceSession: FirInferenceSession, block: () -> R): R {
        val oldSession = this.inferenceSession
        this.inferenceSession = inferenceSession
        return try {
            block()
        } finally {
            this.inferenceSession = oldSession
        }
    }

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

    fun getPrimaryConstructorPureParametersScope(): FirLocalScope? =
        towerDataContextsForClassParts.primaryConstructorPureParametersScope

    fun getPrimaryConstructorAllParametersScope(): FirLocalScope? =
        towerDataContextsForClassParts.primaryConstructorAllParametersScope

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

    @OptIn(PrivateForInline::class)
    inline fun <R> withTowerDataCleanup(l: () -> R): R {
        val initialContext = towerDataContext
        return try {
            l()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    inline fun <T> withTowerDataMode(mode: FirTowerDataMode, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataMode = mode
            f()
        }
    }

    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: FirAnonymousFunctionSymbol, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataContextsForClassParts.setAnonymousFunctionContext(symbol)
            f()
        }
    }

    inline fun <T> withCallableReferenceTowerDataContext(access: FirCallableReferenceAccess, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataContextsForClassParts.setCallableReferenceContextIfAny(access)
            f()
        }
    }

    inline fun <R> withTowerModeCleanup(l: () -> R): R {
        val initialMode = towerDataMode
        return try {
            l()
        } finally {
            towerDataMode = initialMode
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withLambdaBeingAnalyzedInDependentContext(lambda: FirAnonymousFunctionSymbol, l: () -> R): R {
        anonymousFunctionsAnalyzedInDependentContext.add(lambda)
        return try {
            l()
        } finally {
            anonymousFunctionsAnalyzedInDependentContext.remove(lambda)
        }
    }

    var towerDataMode: FirTowerDataMode
        get() = towerDataContextsForClassParts.mode
        set(value) {
            towerDataContextsForClassParts.mode = value
        }

    @OptIn(PrivateForInline::class)
    fun replaceTowerDataContext(newContext: FirTowerDataContext) {
        towerDataContextsForClassParts.currentContext = newContext
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

    fun storeBackingField(property: FirProperty) {
        updateLastScope { storeBackingField(property) }
    }

    fun saveContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        towerDataContextForAnonymousFunctions[anonymousFunction.symbol] = towerDataContext
    }

    fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        towerDataContextForAnonymousFunctions.remove(anonymousFunction.symbol)
    }

    fun clear() {
        towerDataContextForAnonymousFunctions.clear()
        towerDataContextForCallableReferences.clear()
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
    ): BodyResolveContext =
        BodyResolveContext(returnTypeCalculator, dataFlowAnalyzerContext, targetedLocalClasses, outerLocalClassForNested).apply {
            file = this@BodyResolveContext.file
            towerDataContextForAnonymousFunctions.putAll(this@BodyResolveContext.towerDataContextForAnonymousFunctions)
            towerDataContextForCallableReferences.putAll(this@BodyResolveContext.towerDataContextForCallableReferences)
            containers = this@BodyResolveContext.containers
            replaceTowerDataContext(this@BodyResolveContext.towerDataContext)
            anonymousFunctionsAnalyzedInDependentContext.addAll(this@BodyResolveContext.anonymousFunctionsAnalyzedInDependentContext)
        }

    // WITH FirElement functions

    internal inline fun <T> withFile(
        file: FirFile,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        clear()
        @OptIn(PrivateForInline::class)
        this.file = file
        return withScopeCleanup(mutableFileImportsScope) {
            withTowerDataCleanup {
                val importingScopes = createImportingScopes(file, holder.session, holder.scopeSession)
                mutableFileImportsScope += importingScopes
                addNonLocalTowerDataElements(importingScopes.map { it.asTowerDataElement(isLocal = false) })
                f()
            }
        }
    }

    inline fun <T> withRegularClass(
        regularClass: FirRegularClass,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        storeClassIfNotNested(regularClass)
        return withTowerModeCleanup {
            if (!regularClass.isInner && containerIfAny is FirRegularClass) {
                towerDataMode = if (regularClass.isCompanion) {
                    FirTowerDataMode.COMPANION_OBJECT
                } else {
                    FirTowerDataMode.NESTED_CLASS
                }
            }

            withScopesForClass(regularClass.name, regularClass, regularClass.defaultType(), holder) {
                f()
            }
        }
    }

    inline fun <T> withScopesForClass(
        labelName: Name?,
        owner: FirClass<*>,
        type: ConeKotlinType,
        holder: SessionHolder,
        f: () -> T
    ): T {
        val towerElementsForClass = holder.collectTowerDataElementsForClass(owner, type)

        val base = towerDataContext.addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticsAndCompanionReceivers)
        val statics = base
            .addNonLocalScopeIfNotNull(towerElementsForClass.companionStaticScope)
            .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)

        val companionReceiver = towerElementsForClass.companionReceiver
        val staticsAndCompanion = if (companionReceiver == null) statics else base
            .addReceiver(null, companionReceiver)
            .addNonLocalScopeIfNotNull(towerElementsForClass.companionStaticScope)
            .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)

        val typeParameterScope = (owner as? FirRegularClass)?.let(this::createTypeParameterScope)

        val forMembersResolution =
            staticsAndCompanion
                .addReceiver(labelName, towerElementsForClass.thisReceiver)
                .addNonLocalScopeIfNotNull(typeParameterScope)

        val scopeForConstructorHeader =
            staticsAndCompanion.addNonLocalScopeIfNotNull(typeParameterScope)

        val newTowerDataContextForStaticNestedClasses =
            if ((owner as? FirRegularClass)?.classKind?.isSingleton == true)
                forMembersResolution
            else
                staticsAndCompanion

        val constructor = (owner as? FirRegularClass)?.declarations?.firstOrNull { it is FirConstructor } as? FirConstructor
        val (primaryConstructorPureParametersScope, primaryConstructorAllParametersScope) =
            if (constructor?.isPrimary == true) {
                constructor.scopesWithPrimaryConstructorParameters(owner)
            } else {
                null to null
            }

        val newContexts = FirTowerDataContextsForClassParts(
            forMembersResolution,
            newTowerDataContextForStaticNestedClasses,
            statics,
            scopeForConstructorHeader,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope
        )

        return withNewTowerDataForClassParts(newContexts) {
            f()
        }
    }

    fun createTypeParameterScope(declaration: FirMemberDeclaration): FirMemberTypeParameterScope? {
        if (declaration.typeParameters.isEmpty()) return null
        return FirMemberTypeParameterScope(declaration)
    }

    fun FirConstructor.scopesWithPrimaryConstructorParameters(
        ownerClass: FirClass<*>
    ): Pair<FirLocalScope, FirLocalScope> {
        var parameterScope = FirLocalScope()
        var allScope = FirLocalScope()
        val properties = ownerClass.declarations.filterIsInstance<FirProperty>().associateBy { it.name }
        for (parameter in valueParameters) {
            allScope = allScope.storeVariable(parameter)
            val property = properties[parameter.name]
            if (property?.source?.kind != FirFakeSourceElementKind.PropertyFromParameter) {
                parameterScope = parameterScope.storeVariable(parameter)
            }
        }
        return parameterScope to allScope
    }
}
