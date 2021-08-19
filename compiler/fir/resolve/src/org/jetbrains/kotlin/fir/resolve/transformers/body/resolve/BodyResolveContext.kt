/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.primaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
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
import org.jetbrains.kotlin.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR

class BodyResolveContext(
    val returnTypeCalculator: ReturnTypeCalculator,
    val dataFlowAnalyzerContext: DataFlowAnalyzerContext<PersistentFlow>,
    val targetedLocalClasses: Set<FirClassLikeDeclaration> = emptySet(),
    val outerLocalClassForNested: MutableMap<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>> = mutableMapOf()
) {
    val fileImportsScope: MutableList<FirScope> = mutableListOf()

    @set:PrivateForInline
    lateinit var file: FirFile

    var towerDataContextsForClassParts = FirTowerDataContextsForClassParts(forMemberDeclarations = FirTowerDataContext())
        private set

    val towerDataContext: FirTowerDataContext
        get() = towerDataContextsForClassParts.currentContext

    @OptIn(PrivateForInline::class)
    var towerDataMode: FirTowerDataMode
        get() = towerDataContextsForClassParts.mode
        set(value) {
            towerDataContextsForClassParts.mode = value
        }

    val implicitReceiverStack: ImplicitReceiverStack
        get() = towerDataContext.implicitReceiverStack

    private val towerDataContextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, FirTowerDataContext>
        get() = towerDataContextsForClassParts.towerDataContextForAnonymousFunctions

    private val towerDataContextForCallableReferences: MutableMap<FirCallableReferenceAccess, FirTowerDataContext>
        get() = towerDataContextsForClassParts.towerDataContextForCallableReferences

    @set:PrivateForInline
    var containers: ArrayDeque<FirDeclaration> = ArrayDeque()

    @set:PrivateForInline
    var containingClass: FirRegularClass? = null

    val containerIfAny: FirDeclaration?
        get() = containers.lastOrNull()

    @set:PrivateForInline
    var inferenceSession: FirInferenceSession = FirInferenceSession.DEFAULT

    val anonymousFunctionsAnalyzedInDependentContext: MutableSet<FirFunctionSymbol<*>> = mutableSetOf()

    var containingClassDeclarations: ArrayDeque<FirRegularClass> = ArrayDeque()

    val topClassDeclaration: FirRegularClass?
        get() = containingClassDeclarations.lastOrNull()

    private inline fun <T> withNewTowerDataForClassParts(newContexts: FirTowerDataContextsForClassParts, f: () -> T): T {
        val old = towerDataContextsForClassParts
        towerDataContextsForClassParts = newContexts
        return try {
            f()
        } finally {
            towerDataContextsForClassParts = old
        }
    }

    private inline fun <R> withLambdaBeingAnalyzedInDependentContext(lambda: FirAnonymousFunctionSymbol, l: () -> R): R {
        anonymousFunctionsAnalyzedInDependentContext.add(lambda)
        return try {
            l()
        } finally {
            anonymousFunctionsAnalyzedInDependentContext.remove(lambda)
        }
    }

    @PrivateForInline
    inline fun <T> withContainer(declaration: FirDeclaration, f: () -> T): T {
        containers.add(declaration)
        return try {
            f()
        } finally {
            containers.removeLast()
        }
    }

    @PrivateForInline
    private inline fun <T> withContainerClass(declaration: FirRegularClass, f: () -> T): T {
        val oldContainingClass = containingClass
        containers.add(declaration)
        containingClass = declaration
        return try {
            f()
        } finally {
            containers.removeLast()
            containingClass = oldContainingClass
        }
    }

    inline fun <T> withContainingClass(declaration: FirRegularClass, f: () -> T): T {
        containingClassDeclarations.add(declaration)
        return try {
            f()
        } finally {
            containingClassDeclarations.removeLast()
        }
    }

    @PrivateForInline
    inline fun <R> withTowerDataCleanup(l: () -> R): R {
        val initialContext = towerDataContext
        return try {
            l()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    @PrivateForInline
    inline fun <T> withTowerDataMode(mode: FirTowerDataMode, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataMode = mode
            f()
        }
    }

    @PrivateForInline
    inline fun <R> withTowerModeCleanup(l: () -> R): R {
        val initialMode = towerDataMode
        return try {
            l()
        } finally {
            towerDataMode = initialMode
        }
    }

    var qualifierPartIndexFromEnd: Int = -1

    inline fun <R> withIncrementedQualifierPartIndex(l: () -> R): R {
        qualifierPartIndexFromEnd++
        return try {
            l()
        } finally {
            qualifierPartIndexFromEnd--
        }
    }

    @PrivateForInline
    fun replaceTowerDataContext(newContext: FirTowerDataContext) {
        towerDataContextsForClassParts.currentContext = newContext
    }

    @PrivateForInline
    fun clear() {
        towerDataContextForAnonymousFunctions.clear()
        towerDataContextForCallableReferences.clear()
        dataFlowAnalyzerContext.reset()
    }

    @PrivateForInline
    fun addNonLocalTowerDataElement(element: FirTowerDataElement) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(listOf(element)))
    }

    @PrivateForInline
    fun addNonLocalTowerDataElements(newElements: List<FirTowerDataElement>) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(newElements))
    }

    @PrivateForInline
    fun addLocalScope(localScope: FirLocalScope) {
        replaceTowerDataContext(towerDataContext.addLocalScope(localScope))
    }

    @PrivateForInline
    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>) {
        replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue))
    }

    @PrivateForInline
    private inline fun updateLastScope(transform: FirLocalScope.() -> FirLocalScope) {
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        replaceTowerDataContext(towerDataContext.setLastLocalScope(lastScope.transform()))
    }

    @PrivateForInline
    fun storeFunction(function: FirSimpleFunction) {
        updateLastScope { storeFunction(function) }
    }

    @PrivateForInline
    private inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirCallableDeclaration,
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

    @PrivateForInline
    inline fun <T> withTypeParametersOf(declaration: FirMemberDeclaration, l: () -> T): T {
        if (declaration.typeParameters.isEmpty()) return l()
        val scope = FirMemberTypeParameterScope(declaration)
        return withTowerDataCleanup {
            addNonLocalTowerDataElement(scope.asTowerDataElement(isLocal = false))
            l()
        }
    }

    private fun FirMemberDeclaration.typeParameterScope(): FirMemberTypeParameterScope? {
        if (typeParameters.isEmpty()) return null
        return FirMemberTypeParameterScope(this)
    }

    fun buildSecondaryConstructorParametersScope(constructor: FirConstructor): FirLocalScope =
        constructor.valueParameters.fold(FirLocalScope()) { acc, param -> acc.storeVariable(param) }

    @PrivateForInline
    fun addInaccessibleImplicitReceiverValue(
        owningClass: FirRegularClass?,
        holder: SessionHolder,
    ) {
        if (owningClass == null) return
        addReceiver(
            name = null,
            implicitReceiverValue = InaccessibleImplicitReceiverValue(
                owningClass.symbol,
                owningClass.defaultType(),
                holder.session,
                holder.scopeSession
            )
        )
    }

    @PrivateForInline
    private fun storeBackingField(property: FirProperty) {
        updateLastScope { storeBackingField(property) }
    }

    // ANALYSIS PUBLIC API

    fun getPrimaryConstructorPureParametersScope(): FirLocalScope? =
        towerDataContextsForClassParts.primaryConstructorPureParametersScope

    fun getPrimaryConstructorAllParametersScope(): FirLocalScope? =
        towerDataContextsForClassParts.primaryConstructorAllParametersScope

    @OptIn(PrivateForInline::class)
    fun storeClassIfNotNested(klass: FirRegularClass) {
        if (containerIfAny is FirClass) return
        updateLastScope { storeClass(klass) }
    }

    @OptIn(PrivateForInline::class)
    fun storeVariable(variable: FirVariable) {
        updateLastScope { storeVariable(variable) }
    }

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
    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: FirAnonymousFunctionSymbol, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataContextsForClassParts.setAnonymousFunctionContextIfAny(symbol)
            f()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withCallableReferenceTowerDataContext(access: FirCallableReferenceAccess, f: () -> T): T {
        return withTowerModeCleanup {
            towerDataContextsForClassParts.setCallableReferenceContextIfAny(access)
            f()
        }
    }

    fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        towerDataContextForAnonymousFunctions.remove(anonymousFunction.symbol)
    }

    @OptIn(PrivateForInline::class)
    fun createSnapshotForLocalClasses(
        returnTypeCalculator: ReturnTypeCalculator,
        targetedLocalClasses: Set<FirClassLikeDeclaration>
    ): BodyResolveContext =
        BodyResolveContext(returnTypeCalculator, dataFlowAnalyzerContext, targetedLocalClasses, outerLocalClassForNested).apply {
            file = this@BodyResolveContext.file
            towerDataContextForAnonymousFunctions.putAll(this@BodyResolveContext.towerDataContextForAnonymousFunctions)
            towerDataContextForCallableReferences.putAll(this@BodyResolveContext.towerDataContextForCallableReferences)
            containers = this@BodyResolveContext.containers
            replaceTowerDataContext(this@BodyResolveContext.towerDataContext)
            anonymousFunctionsAnalyzedInDependentContext.addAll(this@BodyResolveContext.anonymousFunctionsAnalyzedInDependentContext)
        }

    // withElement PUBLIC API

    @OptIn(PrivateForInline::class)
    inline fun <T> withFile(
        file: FirFile,
        holder: SessionHolder,
        f: () -> T
    ): T {
        clear()
        this.file = file
        return withScopeCleanup(fileImportsScope) {
            withTowerDataCleanup {
                val importingScopes = createImportingScopes(file, holder.session, holder.scopeSession)
                fileImportsScope += importingScopes
                addNonLocalTowerDataElements(importingScopes.map { it.asTowerDataElement(isLocal = false) })
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withRegularClass(
        regularClass: FirRegularClass,
        holder: SessionHolder,
        forContracts: Boolean = false,
        f: () -> T
    ): T {
        storeClassIfNotNested(regularClass)
        if (forContracts) {
            return withTypeParametersOf(regularClass) {
                withContainerClass(regularClass, f)
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
                withContainerClass(regularClass, f)
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousObject(
        anonymousObject: FirAnonymousObject,
        holder: SessionHolder,
        crossinline f: () -> T
    ): T {
        return withScopesForClass(anonymousObject, holder) {
            withContainer(anonymousObject, f)
        }
    }

    fun <T> withScopesForClass(
        owner: FirClass,
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

        /*
         * Scope for enum entries is equal to initial scope for constructor header
         *
         * The only difference that we add value parameters to local scope for constructors
         *   and should not do this for enum entries
         */

        @Suppress("UnnecessaryVariable")
        val scopeForEnumEntries = scopeForConstructorHeader

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
            scopeForEnumEntries,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope
        )

        return withNewTowerDataForClassParts(newContexts) {
            f()
        }
    }

    private fun FirConstructor.scopesWithPrimaryConstructorParameters(
        ownerClass: FirClass
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

    @OptIn(PrivateForInline::class)
    inline fun <T> withSimpleFunction(
        simpleFunction: FirSimpleFunction,
        f: () -> T
    ): T {
        if (containerIfAny !is FirClass) {
            storeFunction(simpleFunction)
        }

        return withTypeParametersOf(simpleFunction) {
            withContainer(simpleFunction, f)
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> forFunctionBody(
        function: FirFunction,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope())
            if (function is FirSimpleFunction) {
                // Make all value parameters available in the local scope so that even one parameter that refers to another parameter,
                // which may not be initialized yet, can be resolved. [FirFunctionParameterChecker] will detect and report an error
                // if an uninitialized parameter is accessed by a preceding parameter.
                for (parameter in function.valueParameters) {
                    storeVariable(parameter)
                }
                val receiverTypeRef = function.receiverTypeRef
                withLabelAndReceiverType(function.name, function, receiverTypeRef?.coneType, holder, f)
            } else {
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> forConstructorBody(
        constructor: FirConstructor,
        f: () -> T
    ): T {
        return if (constructor.isPrimary) {
            /*
             * Primary constructor may have body only if class delegates implementation to some property
             *   In it's body we don't have this receiver for building class, so we need to use
             *   special towerDataContext
             */
            withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
                getPrimaryConstructorAllParametersScope()?.let { addLocalScope(it) }
                f()
            }
        } else {
            withTowerDataCleanup {
                addLocalScope(buildSecondaryConstructorParametersScope(constructor))
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        holder: SessionHolder,
        mode: ResolutionMode,
        f: () -> T
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

    @OptIn(PrivateForInline::class)
    inline fun <T> withField(
        field: FirField,
        f: () -> T
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

    @OptIn(PrivateForInline::class)
    inline fun <T> forEnumEntry(
        f: () -> T
    ): T = withTowerDataMode(FirTowerDataMode.ENUM_ENTRY, f)

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        f: () -> T
    ): T {
        return withTowerDataCleanup {
            getPrimaryConstructorPureParametersScope()?.let { addLocalScope(it) }
            addLocalScope(FirLocalScope())
            withContainer(anonymousInitializer, f)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withValueParameter(
        valueParameter: FirValueParameter,
        f: () -> T
    ): T {
        if (!valueParameter.name.isSpecial || valueParameter.name != UNDERSCORE_FOR_UNUSED_VAR) {
            storeVariable(valueParameter)
        }
        return withContainer(valueParameter, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withProperty(
        property: FirProperty,
        f: () -> T
    ): T {
        return withTypeParametersOf(property) {
            withContainer(property, f)
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withPropertyAccessor(
        property: FirProperty,
        accessor: FirPropertyAccessor,
        holder: SessionHolder,
        forContracts: Boolean = false,
        f: () -> T
    ): T {
        if (accessor is FirDefaultPropertyAccessor || accessor.body == null) {
            return if (accessor.isGetter) withContainer(accessor, f)
            else withTowerDataCleanup {
                addLocalScope(FirLocalScope())
                withContainer(accessor, f)
            }
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

    @OptIn(PrivateForInline::class)
    inline fun <T> forPropertyInitializer(f: () -> T): T {
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
        f: FirDelegatedPropertyInferenceSession.() -> T
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

    @OptIn(PrivateForInline::class)
    inline fun <T> withConstructor(constructor: FirConstructor, f: () -> T): T =
        withContainer(constructor, f)

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParameters(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        f: () -> T
    ): T {
        // Default values of constructor can't access members of constructing class
        // But, let them get resolved, then [FirFunctionParameterChecker] will detect and report an error
        // if an uninitialized parameter is accessed by a preceding parameter.
        return forConstructorParametersOrDelegatedConstructorCall(constructor, owningClass, holder, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forDelegatedConstructorCall(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return forConstructorParametersOrDelegatedConstructorCall(constructor, owningClass, holder, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParametersOrDelegatedConstructorCall(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
            if (constructor.isPrimary) {
                getPrimaryConstructorAllParametersScope()?.let {
                    withTowerDataCleanup {
                        addLocalScope(it)
                        f()
                    }
                } ?: f()
            } else {
                addInaccessibleImplicitReceiverValue(owningClass, holder)
                withTowerDataCleanup {
                    addLocalScope(buildSecondaryConstructorParametersScope(constructor))
                    constructor.valueParameters.forEach { storeVariable(it) }
                    f()
                }
            }
        }
    }

    fun storeCallableReferenceContext(callableReferenceAccess: FirCallableReferenceAccess) {
        towerDataContextForCallableReferences[callableReferenceAccess] = towerDataContext
    }

    fun dropCallableReferenceContext(callableReferenceAccess: FirCallableReferenceAccess) {
        towerDataContextForCallableReferences.remove(callableReferenceAccess)
    }

    fun <T> withWhenExpression(whenExpression: FirWhenExpression, f: () -> T): T {
        if (whenExpression.subjectVariable == null) return f()
        return forBlock(f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forBlock(f: () -> T): T {
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope())
            f()
        }
    }
}
