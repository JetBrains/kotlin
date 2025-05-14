/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.replSnippetResolveExtensions
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.withScopeCleanup
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.computeImportingScopes
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.scopes.impl.FirMemberTypeParameterScope
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
import org.jetbrains.kotlin.util.PrivateForInline

class BodyResolveContext(
    @set:PrivateForInline
    var returnTypeCalculator: ReturnTypeCalculator,
    val dataFlowAnalyzerContext: DataFlowAnalyzerContext,
) {
    val fileImportsScope: MutableList<FirScope> = mutableListOf()

    @set:PrivateForInline
    lateinit var file: FirFile

    @PrivateForInline
    var regularTowerDataContexts: FirRegularTowerDataContexts = FirRegularTowerDataContexts(regular = FirTowerDataContext())

    // TODO: Rename to postponed
    @PrivateForInline
    val specialTowerDataContexts: FirSpecialTowerDataContexts = FirSpecialTowerDataContexts()

    @OptIn(PrivateForInline::class)
    val towerDataContext: FirTowerDataContext
        get() = regularTowerDataContexts.currentContext
            ?: throw AssertionError("No regular data context found, towerDataMode = $towerDataMode")

    @OptIn(PrivateForInline::class)
    var towerDataMode: FirTowerDataMode
        get() = regularTowerDataContexts.activeMode
        set(value) {
            regularTowerDataContexts = regularTowerDataContexts.replaceTowerDataMode(newMode = value)
        }

    val implicitValueStorage: ImplicitValueStorage
        get() = towerDataContext.implicitValueStorage

    @set:PrivateForInline
    var containers: ArrayDeque<FirDeclaration> = ArrayDeque()

    val topContainerForTypeResolution: FirDeclaration?
        get() = containers.lastOrNull { it is FirTypeParameterRefsOwner && it !is FirAnonymousFunction }

    @set:PrivateForInline
    var containingRegularClass: FirRegularClass? = null

    val containerIfAny: FirDeclaration?
        get() = containers.lastOrNull()

    @set:PrivateForInline
    var inferenceSession: FirInferenceSession = FirInferenceSession.DEFAULT

    @set:PrivateForInline
    var isInsideAssignmentRhs: Boolean = false

    @OptIn(PrivateForInline::class)
    inline fun <R> withAssignmentRhs(block: () -> R): R {
        val oldMode = this.isInsideAssignmentRhs
        this.isInsideAssignmentRhs = true
        return try {
            block()
        } finally {
            this.isInsideAssignmentRhs = oldMode
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun withClassHeader(clazz: FirRegularClass, action: () -> Unit) {
        withSwitchedTowerDataModeForStaticNestedClass(clazz) {
            withContainer(clazz, action)
        }
    }

    val anonymousFunctionsAnalyzedInDependentContext: MutableSet<FirFunctionSymbol<*>> = mutableSetOf()

    var containingClassDeclarations: ArrayDeque<FirClass> = ArrayDeque()

    @set:PrivateForInline
    var targetedLocalClasses: Set<FirClassLikeDeclaration> = emptySet()

    val outerLocalClassForNested: MutableMap<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>> = mutableMapOf()

    @OptIn(PrivateForInline::class)
    inline fun <T> withTowerDataContexts(newContexts: FirRegularTowerDataContexts, f: () -> T): T {
        val old = regularTowerDataContexts
        regularTowerDataContexts = newContexts
        return try {
            f()
        } finally {
            regularTowerDataContexts = old
        }
    }

    inline fun <R> withLambdaBeingAnalyzedInDependentContext(lambda: FirAnonymousFunctionSymbol, l: () -> R): R {
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
    private inline fun <T> withContainerRegularClass(declaration: FirRegularClass, f: () -> T): T {
        val oldContainingClass = containingRegularClass
        containers.add(declaration)
        containingRegularClass = declaration
        return try {
            f()
        } finally {
            containers.removeLast()
            containingRegularClass = oldContainingClass
        }
    }

    inline fun <T> withContainingClass(declaration: FirClass, f: () -> T): T {
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
        return withTowerDataModeCleanup {
            towerDataMode = mode
            f()
        }
    }

    @PrivateForInline
    inline fun <R> withTowerDataModeCleanup(l: () -> R): R {
        val initialMode = towerDataMode
        return try {
            l()
        } finally {
            towerDataMode = initialMode
        }
    }

    @PrivateForInline
    fun replaceTowerDataContext(newContext: FirTowerDataContext) {
        regularTowerDataContexts = regularTowerDataContexts.replaceCurrentlyActiveContext(newContext)
    }

    @PrivateForInline
    fun clear() {
        specialTowerDataContexts.clear()
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
    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiver<*>, additionalLabelName: Name? = null) {
        replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue, additionalLabelName))
    }

    @PrivateForInline
    fun addAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        replaceTowerDataContext(towerDataContext.addAnonymousInitializer(anonymousInitializer))
    }

    @PrivateForInline
    private inline fun updateLastScope(transform: FirLocalScope.() -> FirLocalScope) {
        val lastScope = towerDataContext.localScopes.lastOrNull() ?: return
        replaceTowerDataContext(towerDataContext.setLastLocalScope(lastScope.transform()))
    }

    @PrivateForInline
    fun storeFunction(function: FirSimpleFunction, session: FirSession) {
        updateLastScope { storeFunction(function, session) }
    }

    @PrivateForInline
    private inline fun <T> withLabelAndReceiverType(
        labelName: Name?,
        owner: FirCallableDeclaration,
        type: ConeKotlinType?,
        staticReceiver: FirClassLikeSymbol<*>?,
        holder: SessionHolder,
        additionalLabelName: Name? = null,
        f: () -> T
    ): T = withTowerDataCleanup {
        val contextReceivers = mutableListOf<ContextReceiverValue>()
        val contextParameters = mutableListOf<ImplicitContextParameterValue>()

        owner.contextParameters.forEach { receiver ->
            if (receiver.isLegacyContextReceiver()) {
                contextReceivers += ContextReceiverValue(
                    receiver.symbol, receiver.returnTypeRef.coneType, receiver.name, holder.session, holder.scopeSession,
                )
            } else {
                contextParameters += ImplicitContextParameterValue(receiver.symbol, receiver.returnTypeRef.coneType)
            }
        }

        replaceTowerDataContext(towerDataContext.addContextGroups(contextReceivers, contextParameters))

        if (type != null) {
            val receiver = ImplicitExtensionReceiverValue(
                owner.receiverParameter!!.symbol,
                type,
                holder.session,
                holder.scopeSession
            )
            addReceiver(labelName, receiver, additionalLabelName)
        }

        if (staticReceiver != null) {
            val phantomStaticReceiver = PhantomStaticThis(
                staticReceiver,
                holder.session,
                holder.scopeSession
            )
            addReceiver(labelName, phantomStaticReceiver, additionalLabelName)
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

    fun buildConstructorParametersScope(constructor: FirConstructor, session: FirSession): FirLocalScope =
        constructor.valueParameters.fold(FirLocalScope(session)) { acc, param -> acc.storeVariable(param, session) }

    @PrivateForInline
    fun addInaccessibleImplicitReceiverValue(
        owningClass: FirRegularClass?,
        holder: SessionHolder,
    ) {
        if (owningClass == null) return
        addReceiver(
            name = owningClass.name,
            implicitReceiverValue = InaccessibleImplicitReceiverValue(
                owningClass.symbol,
                owningClass.defaultType(),
                holder.session,
                holder.scopeSession
            )
        )
    }

    @PrivateForInline
    private fun storeBackingField(property: FirProperty, session: FirSession) {
        updateLastScope { storeBackingField(property, session) }
    }

    // ANALYSIS PUBLIC API

    /**
     * Pure parameters are those that are not properties.
     * For example, in code `constructor(p1: Int, val p2: Int)` only `p1` is a pure parameter.
     *
     * To be used in contexts, where pure primary constructor parameters are accessible, e.g., property initializers.
     * In primary constructor itself create new scope using [buildConstructorParametersScope].
     */
    @OptIn(PrivateForInline::class)
    fun getPrimaryConstructorPureParametersScope(): FirLocalScope? =
        regularTowerDataContexts.primaryConstructorPureParametersScope

    /**
     * To be used in contexts, where primary constructor parameters are accessible, e.g., supertype delegate expression.
     * In primary constructor itself create new scope using [buildConstructorParametersScope].
     */
    @OptIn(PrivateForInline::class)
    fun getPrimaryConstructorAllParametersScope(): FirLocalScope? =
        regularTowerDataContexts.primaryConstructorAllParametersScope

    @OptIn(PrivateForInline::class)
    fun storeClassIfNotNested(klass: FirRegularClass, session: FirSession) {
        if (containerIfAny is FirClass) return
        updateLastScope { storeClass(klass, session) }
    }

    @OptIn(PrivateForInline::class)
    fun storeVariable(variable: FirVariable, session: FirSession) {
        updateLastScope { storeVariable(variable, session) }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R, S : FirInferenceSession> withInferenceSession(inferenceSession: S, block: S.() -> R): R {
        val oldSession = this.inferenceSession
        this.inferenceSession = inferenceSession
        return try {
            inferenceSession.block()
        } finally {
            this.inferenceSession = oldSession
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: FirAnonymousFunctionSymbol, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getAnonymousFunctionContext(symbol), f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withCallableReferenceTowerDataContext(access: FirCallableReferenceAccess, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getCallableReferenceContext(access), f)
    }

    @PrivateForInline
    inline fun <T> withTemporaryRegularContext(newContext: PostponedAtomsResolutionContext?, f: () -> T): T {
        val (towerDataContext, newInferenceSession) = newContext ?: return f()

        return withTowerDataModeCleanup {
            withTowerDataContexts(regularTowerDataContexts.replaceAndSetActiveRegularContext(towerDataContext)) {
                if (newInferenceSession !== this.inferenceSession) {
                    withInferenceSession(newInferenceSession) { f() }
                } else {
                    f()
                }
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun dropContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        specialTowerDataContexts.dropAnonymousFunctionContext(anonymousFunction.symbol)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forLocalClasses(
        returnTypeCalculator: ReturnTypeCalculator,
        targetedLocalClasses: Set<FirClassLikeDeclaration>,
        f: () -> T,
    ): T {
        val oldReturnTypeCalculator = this.returnTypeCalculator
        val oldTargetedLocalClasses = this.targetedLocalClasses
        return try {
            this.returnTypeCalculator = returnTypeCalculator
            this.targetedLocalClasses = targetedLocalClasses
            f()
        } finally {
            this.returnTypeCalculator = oldReturnTypeCalculator
            this.targetedLocalClasses = oldTargetedLocalClasses
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withReturnTypeCalculator(
        returnTypeCalculator: ReturnTypeCalculator,
        f: () -> T,
    ): T {
        val oldReturnTypeCalculator = this.returnTypeCalculator
        return try {
            this.returnTypeCalculator = returnTypeCalculator
            f()
        } finally {
            this.returnTypeCalculator = oldReturnTypeCalculator
        }
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
                withContainer(file, f)
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> forRegularClassBody(
        regularClass: FirRegularClass,
        holder: SessionHolder,
        f: () -> T
    ): T {
        storeClassIfNotNested(regularClass, holder.session)
        return withSwitchedTowerDataModeForStaticNestedClass(regularClass) {
            withScopesForClass(regularClass, holder) {
                withContainerRegularClass(regularClass, f)
            }
        }
    }

    /**
     * It only changes the current base scope for static nested classes/objects,
     * so it wouldn't contain dispatch receiver and members, but only other statically accessible things
     */
    @PrivateForInline
    inline fun <T> withSwitchedTowerDataModeForStaticNestedClass(
        regularClass: FirRegularClass,
        f: () -> T
    ): T {
        return withTowerDataModeCleanup {
            if (!regularClass.isInner && containerIfAny is FirRegularClass) {
                towerDataMode = if (regularClass.isCompanion) {
                    FirTowerDataMode.COMPANION_OBJECT
                } else {
                    FirTowerDataMode.NESTED_CLASS
                }
            }

            f()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forTypeAlias(typeAlias: FirTypeAlias, f: () -> T): T {
        val isInsideAClass = containerIfAny is FirRegularClass
        return withContainer(typeAlias) {
            if (!isInsideAClass) return@withContainer f()
            return withTowerDataModeCleanup {
                // Though inner type aliases are not supported, we may randomly choose to stick to the same behavior as for inner classes:
                // namely, leaving member scope available
                if (!typeAlias.isInner) {
                    towerDataMode = FirTowerDataMode.NESTED_CLASS
                }

                f()
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

    /**
     * Changes to the order of scopes should also be reflected in
     * [org.jetbrains.kotlin.fir.resolve.transformers.FirTypeResolveTransformer.withClassScopes].
     * Otherwise, we get different behavior between type resolve and body resolve phases.
     */
    fun <T> withScopesForClass(
        owner: FirClass,
        holder: SessionHolder,
        f: () -> T
    ): T {
        val labelName = (owner as? FirRegularClass)?.name
            ?: if (owner.classKind == ClassKind.ENUM_ENTRY) {
                owner.primaryConstructorIfAny(holder.session)?.callableId?.className?.shortName()
            } else null
        val type = owner.defaultType()
        val towerElementsForClass = holder.collectTowerDataElementsForClass(owner, type)

        val base = towerDataContext.addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticsAndCompanionReceivers)

        val staticReceiver = towerElementsForClass.staticReceiver
        val statics = base
            .run { if (staticReceiver != null) addReceiver(null, staticReceiver) else this }
            .addNonLocalScopesIfNotNull(towerElementsForClass.companionStaticScope, towerElementsForClass.staticScope)

        val staticsAndCompanion = when (val companionReceiver = towerElementsForClass.companionReceiver) {
            null -> statics
            else -> base
                .addReceiver(null, companionReceiver)
                .addNonLocalScopesIfNotNull(towerElementsForClass.companionStaticScope, towerElementsForClass.staticScope)
        }

        val typeParameterScope = (owner as? FirRegularClass)?.typeParameterScope()

        // Type parameters must be inserted before all of staticsAndCompanion.
        // Optimization: Only rebuild all of staticsAndCompanion that's below type parameters if there are any type parameters.
        // Otherwise, reuse staticsAndCompanion.
        val forConstructorHeader = if (typeParameterScope != null) {
            towerDataContext
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticsAndCompanionReceivers)
                .run { towerElementsForClass.companionReceiver?.let { addReceiver(null, it) } ?: this }
                .addNonLocalScopesIfNotNull(towerElementsForClass.companionStaticScope, towerElementsForClass.staticScope)
                // Note: scopes here are in reverse order, so type parameter scope is the most prioritized
                .addNonLocalScope(typeParameterScope)
        } else {
            staticsAndCompanion
        }

        val forMembersResolution = forConstructorHeader
            .addReceiver(labelName, towerElementsForClass.thisReceiver)
            .addContextGroups(towerElementsForClass.contextReceivers, emptyList())

        /*
         * Scope for enum entries is equal to initial scope for constructor header
         *
         * The only difference is that we add value parameters to local scope for constructors
         *   and should not do this for enum entries
         */

        @Suppress("UnnecessaryVariable")
        val scopeForEnumEntries = forConstructorHeader

        val newTowerDataContextForStaticNestedClasses =
            if ((owner as? FirRegularClass)?.classKind?.isSingleton == true)
                forMembersResolution
            else
                staticsAndCompanion

        val constructor = (owner as? FirRegularClass)?.declarations?.firstOrNull { it is FirConstructor } as? FirConstructor
        val (primaryConstructorPureParametersScope, primaryConstructorAllParametersScope) =
            if (constructor?.isPrimary == true) {
                constructor.scopesWithPrimaryConstructorParameters(holder.session)
            } else {
                null to null
            }

        val newContexts = FirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = newTowerDataContextForStaticNestedClasses,
            forCompanionObject = statics,
            forConstructorHeaders = forConstructorHeader,
            forEnumEntries = scopeForEnumEntries,
            primaryConstructorPureParametersScope = primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope = primaryConstructorAllParametersScope
        )

        return withTowerDataContexts(newContexts) {
            f()
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withScript(
        owner: FirScript,
        holder: SessionHolder,
        f: () -> T
    ): T {
        val towerElementsForScript = holder.collectTowerDataElementsForScript(owner)

        val base = towerDataContext.addNonLocalTowerDataElements(emptyList())
        val statics = base
            // TODO: temporary solution for avoiding problem described in KT-62712, flatten back after fix
            .let { baseCtx ->
                towerElementsForScript.implicitReceivers.fold(baseCtx) { ctx, it ->
                    ctx.addReceiver(it.type.classId?.shortClassName, it)
                }
            }
            .addNonLocalScopeIfNotNull(towerElementsForScript.staticScope)

        val parameterScope = owner.parameters.filter {
            // for compatibility with old script resolve, the parameters that implicitly copied from the base class c-tor are ignored here
            // this quirk should be removed after removing base class support (KT-60449)
            it.origin != FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
        }.fold(FirLocalScope(holder.session)) { scope, parameter ->
            scope.storeVariable(parameter, holder.session)
        }

        val forMembersResolution =
            statics
                .addLocalScope(parameterScope)

        val newContexts = FirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = forMembersResolution,
            forCompanionObject = statics,
            forConstructorHeaders = null,
            forEnumEntries = null,
            primaryConstructorPureParametersScope = null,
            primaryConstructorAllParametersScope = null
        )

        return withTowerDataContexts(newContexts) {
            withContainer(owner) {
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withReplSnippet(
        replSnippet: FirReplSnippet,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return withContainer(replSnippet) {
            withTowerDataCleanup {

                replSnippet.receivers.mapIndexed { index, receiver ->
                    ImplicitReceiverValueForScriptOrSnippet(
                        receiver.symbol,
                        receiver.typeRef.coneType,
                        holder.session,
                        holder.scopeSession,
                    )
                }.asReversed().forEach {
                    val additionalLabelName = it.type.abbreviatedTypeOrSelf.labelName(holder.session)
                    addReceiver(null, it, additionalLabelName)
                }

                // TODO: robuster matching and error reporting on no extension (KT-72969)
                for (resolveExt in holder.session.extensionService.replSnippetResolveExtensions) {
                    val scope = resolveExt.getSnippetScope(replSnippet, holder.session)
                    if (scope != null) {
                        addNonLocalTowerDataElement(scope.asTowerDataElement(isLocal = false))
                        break
                    }
                }

                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withCodeFragment(codeFragment: FirCodeFragment, holder: SessionHolder, f: () -> T): T {
        val codeFragmentContext = codeFragment.codeFragmentContext ?: error("Context is not set for a code fragment")
        val towerDataContext = codeFragmentContext.towerDataContext

        val fragmentImportTowerDataElements = computeImportingScopes(file, holder.session, holder.scopeSession)
            .map { it.asTowerDataElement(isLocal = false) }

        val base = towerDataContext
            // KT-69102: this line can lead to duplicate context receivers in the implicit receiver stack
            .addNonLocalTowerDataElements(towerDataContext.nonLocalTowerDataElements)
            .addNonLocalTowerDataElements(fragmentImportTowerDataElements)

        val baseWithLocalScope = towerDataContext.localScopes.fold(base) { acc, scope -> acc.addLocalScope(scope) }

        val newContext = FirRegularTowerDataContexts(
            regular = baseWithLocalScope,
            forNestedClasses = baseWithLocalScope,
            forCompanionObject = baseWithLocalScope,
            forConstructorHeaders = null,
            forEnumEntries = null,
            primaryConstructorPureParametersScope = null,
            primaryConstructorAllParametersScope = null
        )

        return withTowerDataContexts(newContext) {
            withContainer(codeFragment, f)
        }
    }

    private fun FirConstructor.scopesWithPrimaryConstructorParameters(session: FirSession): Pair<FirLocalScope, FirLocalScope> {
        var parameterScope = FirLocalScope(session)
        var allScope = FirLocalScope(session)
        for (parameter in valueParameters) {
            allScope = allScope.storeVariable(parameter, session)
            if (parameter.correspondingProperty == null) {
                parameterScope = parameterScope.storeVariable(parameter, session)
            }
        }
        return parameterScope to allScope
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withSimpleFunction(
        simpleFunction: FirSimpleFunction,
        session: FirSession,
        f: () -> T
    ): T {
        if (containerIfAny !is FirClass) {
            storeFunction(simpleFunction, session)
        }

        return withTypeParametersOf(simpleFunction) {
            withContainer(simpleFunction, f)
        }
    }

    /**
     * Invokes [f] in the scope which contains parameters such as value, receiver and context.
     * Also contains required implicit receivers.
     */
    @OptIn(PrivateForInline::class)
    fun <T> withParameters(callable: FirCallableDeclaration, holder: SessionHolder, f: () -> T): T = withTowerDataCleanup {
        addLocalScope(FirLocalScope(holder.session))
        for (contextParameter in callable.contextParameters) {
            storeValueParameterIfNeeded(contextParameter, holder.session)
        }

        if (callable is FirFunction) {
            // Make all value parameters available in the local scope so that even one parameter that refers to another parameter,
            // which may not be initialized yet, can be resolved. [FirFunctionParameterChecker] will detect and report an error
            // if an uninitialized parameter is accessed by a preceding parameter.
            for (parameter in callable.valueParameters) {
                storeVariable(parameter, holder.session)
            }
        }

        val receiverTypeRef = callable.receiverParameter?.typeRef
        val type = receiverTypeRef?.coneType
        val additionalLabelName = type?.abbreviatedTypeOrSelf?.labelName(holder.session)
        withLabelAndReceiverType(
            callable.symbol.name,
            callable,
            type,
            callable.staticReceiverParameter?.coneTypeOrNull?.toClassLikeSymbol(holder.session),
            holder,
            additionalLabelName,
            f
        )
    }

    @OptIn(PrivateForInline::class)
    fun <T> forFunctionBody(
        function: FirFunction,
        holder: SessionHolder,
        f: () -> T
    ): T = withTowerDataCleanup {
        if (function is FirSimpleFunction) {
            withParameters(function, holder, f)
        } else {
            addLocalScope(FirLocalScope(holder.session))
            f()
        }
    }

    private fun ConeKotlinType.labelName(session: FirSession): Name? {
        return when {
            !session.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers) -> null
            else -> (this as? ConeLookupTagBasedType)?.lookupTag?.name
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> forConstructorBody(
        constructor: FirConstructor,
        session: FirSession,
        f: () -> T
    ): T {
        // `forConstructorBody` is only allowed inside a `forConstructor` lambda which sets FirTowerDataMode.CONSTRUCTOR_HEADER
        require(towerDataMode == FirTowerDataMode.CONSTRUCTOR_HEADER)
        return if (constructor.isPrimary) {
            /*
             * Primary constructor may have body only if the class delegates implementation to some property.
             * In its body we don't have `this` receiver for building class, so we just use the same tower data mode
             * as set for the constructor header
             */
            withTowerDataCleanup {
                addLocalScope(buildConstructorParametersScope(constructor, session))
                f()
            }
        } else {
            // In the secondary constructor's body, everything including containing class' receiver should be available
            withTowerDataMode(FirTowerDataMode.REGULAR) {
                withTowerDataCleanup {
                    addLocalScope(buildConstructorParametersScope(constructor, session))
                    f()
                }
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun <T> withDanglingModifierList(
        danglingModifierList: FirDanglingModifierList,
        f: () -> T
    ): T = withTowerDataCleanup {
        withContainer(danglingModifierList, f)
    }

    @OptIn(PrivateForInline::class)
    fun <T> withAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope(holder.session))
            val receiverTypeRef = anonymousFunction.receiverParameter?.typeRef
            val labelName = anonymousFunction.label?.name?.let { Name.identifier(it) }
            withContainer(anonymousFunction) {
                withLabelAndReceiverType(labelName, anonymousFunction, receiverTypeRef?.coneType, null, holder) {
                    f()
                }
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun storeContextForAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        specialTowerDataContexts.storeAnonymousFunctionContext(
            anonymousFunction.symbol, towerDataContext, inferenceSession
        )
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
    inline fun <T> withEnumEntry(
        enumEntry: FirEnumEntry,
        f: () -> T
    ): T = withTowerDataMode(FirTowerDataMode.ENUM_ENTRY) {
        withContainer(enumEntry, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousInitializer(
        anonymousInitializer: FirAnonymousInitializer,
        session: FirSession,
        f: () -> T
    ): T {
        return withTowerDataCleanup {
            getPrimaryConstructorPureParametersScope()?.let { addLocalScope(it) }
            addLocalScope(FirLocalScope(session))
            addAnonymousInitializer(anonymousInitializer)
            withContainer(anonymousInitializer, f)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withValueParameter(
        valueParameter: FirValueParameter,
        session: FirSession,
        f: () -> T
    ): T {
        storeValueParameterIfNeeded(valueParameter, session)
        return withContainer(valueParameter, f)
    }

    fun storeValueParameterIfNeeded(valueParameter: FirValueParameter, session: FirSession) {
        if (!valueParameter.isLegacyContextReceiver() && valueParameter.name != UNDERSCORE_FOR_UNUSED_VAR) {
            storeVariable(valueParameter, session)
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withReceiverParameter(
        valueParameter: FirReceiverParameter,
        f: () -> T
    ): T = withContainer(valueParameter, f)

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
    inline fun <T> withVariableAsContainerIfNeeded(
        variable: FirProperty,
        treatAsProperty: Boolean,
        f: () -> T
    ): T {
        return if (treatAsProperty) withProperty(variable, f) else f()
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
                addLocalScope(FirLocalScope(holder.session))
                withContainer(accessor, f)
            }
        }

        return withTowerDataCleanup {
            val receiverTypeRef = property.receiverParameter?.typeRef
            addLocalScope(FirLocalScope(holder.session))
            for (parameter in property.contextParameters) {
                storeValueParameterIfNeeded(parameter, holder.session)
            }

            if (!forContracts && receiverTypeRef == null && property.returnTypeRef !is FirImplicitTypeRef &&
                !property.isLocal && property.delegate == null &&
                property.contextParameters.isEmpty()
            ) {
                storeBackingField(property, holder.session)
            }

            withContainer(accessor) {
                val type = receiverTypeRef?.coneType
                val additionalLabelName = type?.abbreviatedTypeOrSelf?.labelName(holder.session)
                withLabelAndReceiverType(
                    property.name,
                    property,
                    type,
                    property.staticReceiverParameter?.coneTypeOrNull?.toClassLikeSymbol(holder.session),
                    holder,
                    additionalLabelName,
                    f
                )
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

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructor(constructor: FirConstructor, f: () -> T): T =
        withTowerDataMode(FirTowerDataMode.CONSTRUCTOR_HEADER) {
            withContainer(constructor, f)
        }

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
        return forConstructorParametersOrDelegatedConstructorCallChildren(constructor, owningClass, holder, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forDelegatedConstructorCallChildren(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        f: () -> T
    ): T {
        return forConstructorParametersOrDelegatedConstructorCallChildren(constructor, owningClass, holder, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forDelegatedConstructorCallResolution(
        f: () -> T
    ): T {
        // Arguments of a delegated call should be resolved without an implicit receiver of the containing class,
        // i.e., via CONSTRUCTOR_HEADER slice
        require(towerDataMode == FirTowerDataMode.CONSTRUCTOR_HEADER)

        // While having the implicit receiver for the delegation call itself is crucial for inner super class calls
        return withTowerDataMode(FirTowerDataMode.REGULAR) {
            f()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forConstructorParametersOrDelegatedConstructorCallChildren(
        constructor: FirConstructor,
        owningClass: FirRegularClass?,
        holder: SessionHolder,
        f: () -> T
    ): T {
        require(towerDataMode == FirTowerDataMode.CONSTRUCTOR_HEADER)
        return withTowerDataCleanup {
            if (!constructor.isPrimary) {
                addInaccessibleImplicitReceiverValue(owningClass, holder)
            }
            addLocalScope(buildConstructorParametersScope(constructor, holder.session))
            f()
        }
    }

    @OptIn(PrivateForInline::class)
    fun storeCallableReferenceContext(callableReferenceAccess: FirCallableReferenceAccess) {
        specialTowerDataContexts.storeCallableReferenceContext(
            callableReferenceAccess,
            towerDataContext.createSnapshot(keepMutable = false),
            inferenceSession,
        )
    }

    @OptIn(PrivateForInline::class)
    fun dropCallableReferenceContext(callableReferenceAccess: FirCallableReferenceAccess) {
        specialTowerDataContexts.dropCallableReferenceContext(callableReferenceAccess)
    }

    fun <T> withWhenExpression(whenExpression: FirWhenExpression, session: FirSession, f: () -> T): T {
        if (whenExpression.subjectVariable == null) return f()
        return forBlock(session, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> forBlock(session: FirSession, f: () -> T): T {
        return withTowerDataCleanup {
            addLocalScope(FirLocalScope(session))
            f()
        }
    }
}
