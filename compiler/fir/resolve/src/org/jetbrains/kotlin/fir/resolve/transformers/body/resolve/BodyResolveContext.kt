/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitExtensionReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.InaccessibleImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.PersistentFlow
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.FirDelegatedPropertyInferenceSession
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
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.coneType
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
        forContracts: Boolean = false,
        crossinline f: () -> T
    ): T {
        storeClassIfNotNested(regularClass)
        if (forContracts) {
            return withTypeParametersOf(regularClass) {
                withContainer(regularClass, f)
            }
        }
        return withTowerModeCleanup {
            if (!regularClass.isInner && containerIfAny is FirRegularClass) {
                towerDataMode = if (regularClass.isCompanion) {
                    FirTowerDataMode.COMPANION_OBJECT
                } else {
                    FirTowerDataMode.NESTED_CLASS
                }
            }

            withScopesForClass(regularClass, holder) {
                withContainer(regularClass, f)
            }
        }
    }

    inline fun <T> withAnonymousObject(
        anonymousObject: FirAnonymousObject,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        return withScopesForClass(anonymousObject, holder) {
            withContainer(anonymousObject, f)
        }
    }

    inline fun <T> withScopesForClass(
        owner: FirClass<*>,
        holder: SessionHolder,
        f: () -> T
    ): T {
        val labelName = (owner as? FirRegularClass)?.name
            ?: if (owner.classKind == ClassKind.ENUM_ENTRY) {
                owner.primaryConstructor?.symbol?.callableId?.className?.shortName()
            } else null
        val type = owner.defaultType()
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

        val typeParameterScope = (owner as? FirRegularClass)?.typeParameterScope()

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

    inline fun <T> withSimpleFunction(
        simpleFunction: FirSimpleFunction,
        crossinline f: () -> T
    ): T {
        if (containerIfAny !is FirClass<*>) {
            storeFunction(simpleFunction)
        }

        return withTypeParametersOf(simpleFunction) {
            withContainer(simpleFunction, f)
        }
    }

    inline fun <T> forFunctionBody(
        function: FirFunction<*>,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope())
            if (function is FirSimpleFunction) {
                val receiverTypeRef = function.receiverTypeRef
                withLabelAndReceiverType(function.name, function, receiverTypeRef?.coneType, holder, f)
            } else {
                f()
            }
        }
    }

    inline fun <T> forConstructorBody(
        constructor: FirConstructor,
        parametersScope: FirLocalScope?,
        crossinline f: () -> T
    ): T {
        return if (constructor.isPrimary) {
            /*
             * Primary constructor may have body only if class delegates implementation to some property
             *   In it's body we don't have this receiver for building class, so we need to use
             *   special towerDataContext
             */
            withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
                parametersScope?.let { addLocalScope(it) }
                f()
            }
        } else {
            withTowerDataCleanup {
                parametersScope?.let { addLocalScope(it) }
                f()
            }
        }
    }

    inline fun <T> withAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        holder: SessionHolder,
        mode: ResolutionMode,
        crossinline f: () -> T
    ): T {
        if (mode !is ResolutionMode.LambdaResolution) {
            towerDataContextForAnonymousFunctions[anonymousFunction.symbol] = towerDataContext
        }
        if (mode is ResolutionMode.ContextDependent || mode is ResolutionMode.ContextDependentDelegate) {
            return f()
        }
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope())
            val receiverTypeRef = anonymousFunction.receiverTypeRef
            val labelName = anonymousFunction.label?.name?.let { Name.identifier(it) }
            withContainer(anonymousFunction) {
                withLabelAndReceiverType(labelName, anonymousFunction, receiverTypeRef?.coneType, holder) {
                    if (mode is ResolutionMode.LambdaResolution) {
                        withLambdaBeingAnalyzedInDependentContext(anonymousFunction.symbol, f)
                    } else {
                        f()
                    }
                }
            }
        }
    }

    inline fun <T> withField(
        field: FirField,
        crossinline f: () -> T
    ): T {
        return withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
            withContainer(field) {
                withTowerDataCleanup {
                    getPrimaryConstructorAllParametersScope()?.let { addLocalScope(it) }
                    f()
                }
            }
        }
    }

    inline fun <T> forEnumEntry(
        crossinline f: () -> T
    ): T = withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER, f)

    inline fun <T> withAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        crossinline f: () -> T
    ): T {
        return withTowerDataCleanup {
            getPrimaryConstructorPureParametersScope()?.let { addLocalScope(it) }
            addLocalScope(FirLocalScope())
            withContainer(anonymousInitializer, f)
        }
    }

    inline fun <T> withValueParameter(
        valueParameter: FirValueParameter,
        crossinline f: () -> T
    ): T {
        storeVariable(valueParameter)
        return withContainer(valueParameter, f)
    }

    inline fun <T> withProperty(
        property: FirProperty,
        crossinline f: () -> T
    ): T {
        return withTypeParametersOf(property) {
            withContainer(property, f)
        }
    }

    inline fun <T> withPropertyAccessor(
        property: FirProperty,
        accessor: FirPropertyAccessor,
        holder: SessionHolder,
        forContracts: Boolean = false,
        crossinline f: () -> T
    ): T {
        if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
            return withContainer(accessor, f)
        }
        return withTowerDataCleanup {
            val receiverTypeRef = property.receiverTypeRef
            addLocalScope(FirLocalScope())
            if (!forContracts && receiverTypeRef == null && property.returnTypeRef !is FirImplicitTypeRef &&
                !property.isLocal && property.delegate == null
            ) {
                storeBackingField(property)
            }
            withContainer(accessor) {
                withLabelAndReceiverType(property.name, property, receiverTypeRef?.coneType, holder, f)
            }
        }
    }

    inline fun <T> forPropertyInitializer(crossinline f: () -> T): T {
        return withTowerDataCleanup {
            getPrimaryConstructorPureParametersScope()?.let { addLocalScope(it) }
            f()
        }
    }

    inline fun <T> forPropertyDelegateAccessors(
        property: FirProperty,
        delegateExpression: FirExpression,
        resolutionContext: ResolutionContext,
        callCompleter: FirCallCompleter,
        crossinline f: FirDelegatedPropertyInferenceSession.() -> T
    ) {
        val inferenceSession = FirDelegatedPropertyInferenceSession(
            property,
            delegateExpression,
            resolutionContext,
            callCompleter.createPostponedArgumentsAnalyzer(resolutionContext)
        )

        withInferenceSession(inferenceSession) {
            inferenceSession.f()
        }
    }

    inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirCallableDeclaration<*>,
        type: ConeKotlinType?,
        holder: SessionHolder,
        f: () -> T
    ): T = withTowerDataCleanup {
        if (type != null) {
            val receiver = ImplicitExtensionReceiverValue(
                owner.symbol,
                type,
                holder.session,
                holder.scopeSession
            )
            addReceiver(labelName, receiver)
        }

        f()
    }

    inline fun <T> withTypeParametersOf(declaration: FirMemberDeclaration, crossinline l: () -> T): T {
        val scope = declaration.typeParameterScope()
        return withTowerDataCleanup {
            scope?.let { addNonLocalTowerDataElement(it.asTowerDataElement(isLocal = false)) }
            l()
        }
    }

    fun FirMemberDeclaration.typeParameterScope(): FirMemberTypeParameterScope? {
        if (typeParameters.isEmpty()) return null
        return FirMemberTypeParameterScope(this)
    }

    inline fun <T> withConstructor(constructor: FirConstructor, crossinline f: () -> T): T =
        withContainer(constructor, f)

    inline fun <T> forConstructorParameters(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        // Default values of constructor can't access members of constructing class
        return withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
            if (owningClass != null && !constructor.isPrimary) {
                addReceiver(
                    null,
                    InaccessibleImplicitReceiverValue(
                        owningClass.symbol,
                        owningClass.defaultType(),
                        holder.session,
                        holder.scopeSession
                    )
                )
            }
            withTowerDataCleanup {
                addLocalScope(FirLocalScope())
                f()
            }
        }
    }

    inline fun <T> forDelegatedConstructor(
        parametersScope: FirLocalScope?,
        crossinline f: () -> T
    ): T {
        if (parametersScope == null) return f()
        return withTowerDataCleanup {
            addLocalScope(parametersScope)
            f()
        }
    }

    fun buildConstructorParametersScope(constructor: FirConstructor): FirLocalScope? =
        if (constructor.isPrimary) getPrimaryConstructorAllParametersScope()
        else constructor.valueParameters.fold(FirLocalScope()) { acc, param -> acc.storeVariable(param) }
}
