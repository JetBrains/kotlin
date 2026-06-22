/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dependencies.logic

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.dispatchReceiverClassTypeOrNull
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirIncrementDecrementExpression
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirSuperReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.dependencies.AccessibleIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DefaultedFunctionIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.DependencyNodeIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asClassEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asEnumEntryEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asFileEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnclosingEntity.Companion.asObjectEntity
import org.jetbrains.kotlin.fir.resolve.dependencies.EnumEntryIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.FunctionIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticInitializationIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.StaticPropertyIndex
import org.jetbrains.kotlin.fir.resolve.dependencies.collectEnumEntries
import org.jetbrains.kotlin.fir.resolve.dependencies.containingFileSymbol
import org.jetbrains.kotlin.fir.resolve.dependencies.dsl.DependencyGraphBuilder
import org.jetbrains.kotlin.fir.resolve.dependencies.inSameModule
import org.jetbrains.kotlin.fir.resolve.dependencies.isLibraryDeclaration
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FirStub
import org.jetbrains.kotlin.fir.resolve.isFunctionOrSuspendFunctionInvoke
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.getContainingFile
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirOverrideChecker
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ScopeFunctionRequiresPrewarm
import org.jetbrains.kotlin.fir.scopes.anyOverriddenOf
import org.jetbrains.kotlin.fir.scopes.firOverrideChecker
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.toLookupTag
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.SmartSet
import kotlin.collections.emptySet
import kotlin.let

internal class CallSiteVisitor(
    override val session: FirSession,
    override val scopeSession: ScopeSession,
    private val visitedFiles: Set<FirFileSymbol>,
    private val graphBuilder: DependencyGraphBuilder,
) : SessionAndScopeSessionHolder, FirVisitor<Unit, DependencyNodeIndex>() {

    override fun visitElement(element: FirElement, data: DependencyNodeIndex): Unit = Unit

    private inline fun <E : FirElement> E.visit(
        data: DependencyNodeIndex,
        crossinline block: context(DependencyNodeIndex, E) DependencyGraphBuilder.() -> Unit
    ) = when {
        this is FirDeclaration && !symbol.inSameModule() -> {}
        else -> context(data, this@visit) { graphBuilder.block() }
    }

    context(accessingNode: DependencyNodeIndex)
    private fun FirElement.visitRecursively() = accept(this@CallSiteVisitor, accessingNode)

    private val FirBasedSymbol<*>.inVisitedFile: Boolean get() = session.firProvider.getContainingFile(this)?.symbol in visitedFiles

    private fun DependencyGraphBuilder.postponeFileEntity(accessedSymbol: FirCallableSymbol<*>) {
        val enclosingEntity = session.firProvider.getContainingFile(accessedSymbol)?.symbol?.asFileEntity() ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    private fun DependencyGraphBuilder.postponeFileEntity(accessedSymbol: FirClassSymbol<*>) {
        val enclosingEntity = session.firProvider.getContainingFile(accessedSymbol)?.symbol?.asFileEntity() ?: return
        worklist.add(enclosingEntity.beginInitializationIndex)
    }

    context(accessingNode: DependencyNodeIndex, reference: FirExpression)
    private fun DependencyGraphBuilder.referenceNode(node: AccessibleIndex) = apply {
        node.buildNode()
        accessingNode references node
        val possiblyInitializedEndNode = node.lazilyInitialized?.endInitializationIndex ?: return@apply
        possiblyInitializedEndNode.buildNode()
        possiblyInitializedEndNode mayHappenBefore accessingNode
    }

    context(accessingNode: DependencyNodeIndex, callSite: FirExpression)
    private fun DependencyGraphBuilder.callNode(node: FunctionIndex<*>) = apply {
        node.buildNode()
        accessingNode calls node
        val enclosingEntity = node.lazilyInitialized ?: return@apply
        val possiblyInitializedEndNode = enclosingEntity.endInitializationIndex
        if (node is FunctionIndex.Constructor) {
            if (!(enclosingEntity is EnclosingEntity.Class && enclosingEntity.symbol.classKind == ClassKind.ENUM_CLASS)) {
                possiblyInitializedEndNode mustHappenBefore node
            }
        } else {
            possiblyInitializedEndNode mayHappenBefore node
        }
        if (!node.symbol.inVisitedFile) postponeFileEntity(node.symbol)
    }

    /*
     * =============================================
     *             Visiting properties
     * =============================================
     */

    override fun visitProperty(property: FirProperty, data: DependencyNodeIndex): Unit =
        property.visit(data) {
            // Visit only the initializer
            property.initializer?.visitRecursively()
        }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: DependencyNodeIndex) {
        // So far we only care about getters
        if (!propertyAccessor.isGetter) return
        return propertyAccessor.visit(data) { propertyAccessor.body?.visitRecursively() }
    }

    override fun visitBlock(block: FirBlock, data: DependencyNodeIndex): Unit = block.visit(data) {
        block.statements.forEach { stmt -> stmt.visitRecursively() }
    }

    /*
     * =============================================
     *             Visiting functions
     * =============================================
     */

    @OptIn(ScopeFunctionRequiresPrewarm::class)
    private fun FirNamedFunctionSymbol.findOverriddenFunctionWithDefaultArguments(): FirNamedFunctionSymbol? {
        val containingClass = containingClassLookupTag()?.toClassSymbol() ?: return null
        val scope = containingClass.unsubstitutedScope(
            useSiteSession = session,
            scopeSession = scopeSession,
            withForcedTypeCalculator = true,
            memberRequiredPhase = FirResolvePhase.BODY_RESOLVE
        )
        // Pre-warm the class' scope
        scope.processFunctionsByName(name) { }
        // Find the overridden function declaration
        var result: FirNamedFunctionSymbol? = null
        scope.anyOverriddenOf(this) { overridden ->
            if (overridden.valueParameterSymbols.any { it.hasDefaultValue }) {
                result = overridden
                true
            } else false
        }
        return result
    }

    context(accessingNode: DependencyNodeIndex)
    private val FirFunctionSymbol<*>.defaultParametersIfAny: Pair<DefaultedFunctionIndex<*>, Pair<List<IndexedValue<FirValueParameterSymbol>>, List<IndexedValue<FirValueParameterSymbol>>>>?
        get() = when {
            accessingNode is DefaultedFunctionIndex<*> && accessingNode.functionIndex.symbol == this ->
                accessingNode to accessingNode.defaultParameters.asSequence()
                    .withIndex()
                    .partition { it.value.hasDefaultValue }
            else -> null
        }

    override fun visitFunction(function: FirFunction, data: DependencyNodeIndex): Unit =
        function.visit(data) {
            val symbol = function.symbol
            symbol.defaultParametersIfAny?.let { [defaultedIndex, parameters] ->
                val [defaultMissingParameter, nonDefaultMissingParameter] = parameters
                // Add the known default values for missing parameters to the mapping
                defaultMissingParameter.forEach { [_, parameter] ->
                    parameter.fir.defaultValue?.visitRecursively()
                }
                if (nonDefaultMissingParameter.isNotEmpty() && symbol is FirNamedFunctionSymbol) {
                    // We need to find an overridden declaration by the called declaration which defines these missing default values
                    symbol.findOverriddenFunctionWithDefaultArguments()?.let { overriddenFunction ->
                        val indexedOverriddenParameters = overriddenFunction.valueParameterSymbols.asSequence()
                            .withIndex()
                            .filter { it.value.hasDefaultValue }
                            .associate { it.index to it.value }
                        nonDefaultMissingParameter.forEach { [index, _] ->
                            indexedOverriddenParameters[index]?.fir?.defaultValue?.visitRecursively()
                        }
                    }
                }
                // Build the original function (with a stub call-site)
                context(FirStub) { callNode(defaultedIndex.functionIndex) }
            } ?: function.body?.visitRecursively()
        }

    override fun visitNamedFunction(namedFunction: FirNamedFunction, data: DependencyNodeIndex): Unit =
        visitFunction(namedFunction, data)

    // Forward to visitFunction
    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: DependencyNodeIndex): Unit =
        visitFunction(anonymousFunction, data)

    // Should only be visited with its corresponding function contract
    override fun visitAnonymousFunctionExpression(
        anonymousFunctionExpression: FirAnonymousFunctionExpression, data: DependencyNodeIndex
    ): Unit = Unit

    /*
     * =============================================
     *            Visiting initializers
     * =============================================
     */

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: DependencyNodeIndex): Unit =
        anonymousInitializer.visit(data) { anonymousInitializer.body?.visitRecursively() }

    /*
     * =============================================
     *            Visiting constructors
     * =============================================
     */

    override fun visitConstructor(constructor: FirConstructor, data: DependencyNodeIndex): Unit =
        constructor.visit(data) {
            constructor.delegatedConstructor?.visitRecursively()
            constructor.body?.visitRecursively()
        }

    /*
     * =============================================
     *         Visiting qualified accesses
     * =============================================
     */

    private fun FirResolvedQualifier.toEnclosingEntity(): EnclosingEntity<FirRegularClass>? {
        val classSymbol = symbol?.fullyExpandedClass() ?: return null
        return if (resolvedToCompanionObject) {
            classSymbol.resolvedCompanionObjectSymbol?.asObjectEntity(classSymbol.asClassEntity())
        } else if (classSymbol.classKind.isObject) {
            classSymbol.asObjectEntity()
        } else if (classSymbol.isEnumClass) {
            classSymbol.asClassEntity()
        } else {
            null
        }
    }

    context(accessingIndex: DependencyNodeIndex, access: FirQualifiedAccessExpression)
    private inline fun <D : FirCallableDeclaration, S : FirCallableSymbol<D>> DependencyGraphBuilder.accessNode(
        symbol: S,
        crossinline resolveAccess: context(DependencyNodeIndex, FirQualifiedAccessExpression) DependencyGraphBuilder.(S, EnclosingEntity<*>?) -> Unit,
        processCallables: FirScope.(Name, (S) -> Unit) -> Unit,
        crossinline checkOverride: FirOverrideChecker.(S, S) -> Boolean
    ) {
        // If the callable is an extension, ...
        if (symbol.isExtension) {
            // Visit the extension receiver for dependencies
            access.explicitReceiver?.visitRecursively()
            access.extensionReceiver?.visitRecursively()
        }
        val accessingEnclosingEntity = (accessingIndex as? StaticInitializationIndex)?.enclosingEntity
        // Compute the node to this callable based on the access' dispatch receiver
        access.dispatchReceiver?.let { receiver ->
            when (receiver) {
                is FirSuperReceiverExpression -> resolveAccess(symbol, accessingEnclosingEntity)
                is FirThisReceiverExpression -> {
                    val classSymbol = receiver.calleeReference.boundSymbol?.let { symbol ->
                        when (symbol) {
                            is FirReceiverParameterSymbol -> symbol.resolvedType.toClassSymbol()
                            is FirClassSymbol<*> -> symbol
                            is FirTypeAliasSymbol -> symbol.fullyExpandedClass()
                            else -> null
                        }
                    }
                    val accessedEntity = classSymbol?.let { it.asEntity(allowClass = it.isEnumClass && symbol.isStatic) }
                    resolveAccess(symbol, accessedEntity ?: accessingEnclosingEntity)
                }
                is FirResolvedQualifier -> {
                    val enclosingEntity = receiver.toEnclosingEntity() ?: return
                    resolveAccess(symbol, enclosingEntity)
                }
                is FirQualifiedAccessExpression -> {
                    // Either detect an explicit reference to an enum entry or recursively find static accesses in the receiver
                    receiver.calleeReference.toResolvedEnumEntrySymbol(discardErrorReference = true)?.let { enumEntry ->
                        val dispatchReceiverType = symbol.dispatchReceiverClassTypeOrNull() ?: return@let
                        if (!enumEntry.resolvedReturnType.isSubtypeOf(dispatchReceiverType, session)) return@let
                        val enumEntity = enumEntry.asEnumEntryEntity()
                        val enumEntryScope = enumEntry.initializerObjectSymbol
                            ?.declaredMemberScope(session, null)
                            ?: return
                        val overrideChecker = session.firOverrideChecker
                        var foundOverride = false
                        enumEntryScope.processCallables(symbol.name) { callable ->
                            if (!foundOverride && overrideChecker.checkOverride(callable, symbol)) {
                                resolveAccess(callable, enumEntity)
                                foundOverride = true
                            }
                        }
                        if (!foundOverride) resolveAccess(symbol, enumEntity)
                    } ?: run {
                        receiver.visitRecursively()
                        resolveAccess(symbol, null)
                    }
                }
                else -> return
            }
        } ?: symbol.containingFileSymbol?.asFileEntity()?.let { resolveAccess(symbol, it) }
    }

    override fun visitPropertyAccessExpression(
        propertyAccessExpression: FirPropertyAccessExpression,
        data: DependencyNodeIndex
    ): Unit =
        propertyAccessExpression.visit(data) {
            // Case 1: Accessing an enum entry
            propertyAccessExpression.calleeReference.toResolvedEnumEntrySymbol(discardErrorReference = true)?.let {
                referenceNode(EnumEntryIndex(it.asEnumEntryEntity()))
                if (!it.inVisitedFile) postponeFileEntity(it)
            }
            // Case 2: Accessing a property
            propertyAccessExpression.calleeReference.toResolvedPropertySymbol(discardErrorReference = true)?.let { property ->
                accessNode(
                    symbol = property,
                    resolveAccess = resolveAccess@{ propertySymbol, enclosingEntity ->
                        val receiverEntity = enclosingEntity ?: return@resolveAccess
                        val propertyNode = StaticPropertyIndex(receiverEntity, propertySymbol)
                        // If the property has an initializer and no getter, create a reference edge to it
                        if (propertyNode.hasInitializer && propertyNode.getter == null) {
                            referenceNode(propertyNode)
                            if (!propertySymbol.inVisitedFile) {
                                postponeFileEntity(propertySymbol)
                            }
                        }
                        // If the property has a getter, create a call edge to it
                        val propertyGetter = propertyNode.getter ?: return@resolveAccess
                        callNode(propertyGetter)
                    },
                    processCallables = { name, processor ->
                        processPropertiesByName(name) { symbol ->
                            if (symbol is FirPropertySymbol) processor(symbol)
                        }
                    },
                    checkOverride = { overrideCandidate, baseDeclaration ->
                        isOverriddenProperty(overrideCandidate.fir, baseDeclaration.fir)
                    }
                )
            }
        }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: DependencyNodeIndex): Unit =
        resolvedQualifier.visit(data) {
            val symbol = resolvedQualifier.symbol?.fullyExpandedClass() ?: return@visit
            if (symbol.isLibraryDeclaration || !symbol.inSameModule()) return@visit
            // If the qualified class can be a value, ...
            // Only objects can be used as values, enum entries are accessible as properties (variables), and (static) classes are not accessible
            if (resolvedQualifier.canBeValue) {
                val objectEntity = when {
                    resolvedQualifier.resolvedToCompanionObject -> symbol.resolvedCompanionObjectSymbol?.asObjectEntity(symbol.asClassEntity())
                    else -> symbol.asObjectEntity()
                } ?: return@visit
                referenceNode(objectEntity.beginInitializationIndex)
                if (!objectEntity.symbol.inVisitedFile) postponeFileEntity(objectEntity.symbol)
            }
        }

    private fun Map<FirExpression, FirValueParameter>.reverse(): Map<FirValueParameterSymbol, Set<FirExpression>> =
        mutableMapOf<FirValueParameterSymbol, MutableSet<FirExpression>>().apply {
            this@reverse.forEach { [argument, parameter] -> getOrPut(parameter.symbol) { SmartSet.create() }.add(argument) }
        }

    private fun FunctionIndex<*>.defaultedOrSelf(parameters: Set<FirValueParameterSymbol>): FunctionIndex<*> = when (this) {
        is DefaultedFunctionIndex -> this // ignore the input parameters for safety
        else -> if (parameters.isNotEmpty()) DefaultedFunctionIndex(this, parameters) else this
    }

    context(accessingNode: DependencyNodeIndex, accessExpression: FirExpression)
    private fun <T : FirCall> DependencyGraphBuilder.visitArguments(
        symbol: FirFunctionSymbol<*>,
        functionCall: T
    ): Set<FirValueParameterSymbol> {
        val callsInPlaceParameters = symbol.resolvedContractDescription?.effects
            ?.asSequence()
            ?.mapNotNull { it.effect as? ConeCallsEffectDeclaration }
            ?.mapNotNull { symbol.valueParameterSymbols.getOrNull(it.valueParameterReference.parameterIndex) }
            ?.toSet()
            ?: emptySet()
        val argumentMapping = functionCall.resolvedArgumentMapping?.reverse()
            ?: return functionCall.argumentList.visitRecursively().let { emptySet() }
        argumentMapping.forEach { [symbol, expressions] ->
            expressions.forEach { expression ->
                if (expression is FirAnonymousFunctionExpression) {
                    if (symbol in callsInPlaceParameters && expression.anonymousFunction.isLambda) {
                        callNode(FunctionIndex.Closure(expression.anonymousFunction.symbol))
                    }
                } else {
                    expression.visitRecursively()
                }
            }
        }
        return symbol.valueParameterSymbols.filterTo(SmartSet.create()) { it !in argumentMapping }
    }

    private fun FirFunctionCall.propertyAccessFromReceiver(functionSymbol: FirFunctionSymbol<*>): Pair<FirPropertySymbol, FirPropertyAccessExpression>? {
        val access = when {
            functionSymbol.isExtension -> explicitReceiver ?: extensionReceiver
            else -> dispatchReceiver
        } as? FirPropertyAccessExpression ?: return null
        val property = access.calleeReference.toResolvedPropertySymbol(discardErrorReference = true) ?: return null
        return property to access
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: DependencyNodeIndex): Unit =
        functionCall.visit(data) {
            functionCall.calleeReference.toResolvedNamedFunctionSymbol(discardErrorReference = true)?.let { namedFunction ->
                val defaultParameters = visitArguments(namedFunction, functionCall)
                if (namedFunction.isLibraryDeclaration || !namedFunction.inSameModule()) return@visit
                if (namedFunction.callableId.isFunctionOrSuspendFunctionInvoke()) {
                    functionCall.propertyAccessFromReceiver(namedFunction)?.let { [propertySymbol, propertyAccess] ->
                        if (!propertySymbol.resolvedReturnType.isSomeFunctionType(session)) return@let
                        if (defaultParameters.isNotEmpty()) return@visit
                        context(propertyAccess) {
                            accessNode(
                                symbol = propertySymbol,
                                resolveAccess = resolveAccess@{ propertySymbol, enclosingEntity ->
                                    val receiverEntity = enclosingEntity ?: return@resolveAccess
                                    val propertyNode = StaticPropertyIndex(receiverEntity, propertySymbol)
                                    val closure = propertyNode.initializedClosure ?: return@resolveAccess
                                    callNode(closure)
                                },
                                processCallables = { name, processor ->
                                    processPropertiesByName(name) { symbol ->
                                        if (symbol is FirPropertySymbol) processor(symbol)
                                    }
                                },
                                checkOverride = { overrideCandidate, baseDeclaration ->
                                    isOverriddenProperty(overrideCandidate.fir, baseDeclaration.fir)
                                }
                            )
                        }
                    }
                }
                accessNode(
                    symbol = namedFunction,
                    resolveAccess = { functionSymbol, receiverEntity ->
                        val node = FunctionIndex.MemberFunction(functionSymbol, receiverEntity).let {
                            if (defaultParameters.isNotEmpty()) DefaultedFunctionIndex(it, defaultParameters) else it
                        }
                        callNode(node)
                    },
                    processCallables = FirScope::processFunctionsByName,
                    checkOverride = { overrideCandidate, baseDeclaration ->
                        isOverriddenFunction(overrideCandidate.fir, baseDeclaration.fir)
                    }
                )
            }
            functionCall.calleeReference.toResolvedConstructorSymbol(discardErrorReference = true)?.let { constructor ->
                val defaultParameters = visitArguments(constructor, functionCall)
                if (constructor.isLibraryDeclaration || !constructor.inSameModule()) return@visit
                callNode(FunctionIndex.Constructor(constructor).defaultedOrSelf(defaultParameters))
            }
        }

    override fun visitArgumentList(argumentList: FirArgumentList, data: DependencyNodeIndex): Unit =
        argumentList.visit(data) {
            argumentList.arguments.forEach { argument -> argument.visitRecursively() }
        }

    override fun visitDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: DependencyNodeIndex
    ): Unit =
        delegatedConstructorCall.visit(data) {
            val constructor =
                delegatedConstructorCall.calleeReference.toResolvedConstructorSymbol(discardErrorReference = true) ?: return@visit
            val defaultArguments = visitArguments(constructor, delegatedConstructorCall)
            if (constructor.isLibraryDeclaration || !constructor.inSameModule()) return@visit
            callNode(FunctionIndex.Constructor(constructor).defaultedOrSelf(defaultArguments))
        }

    // One should visit the subtypes of the qualified expressions directly
    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: DependencyNodeIndex
    ): Unit = Unit

    /*
     * =============================================
     *         Visiting other expressions
     * =============================================
     */

    override fun visitBooleanOperatorExpression(
        booleanOperatorExpression: FirBooleanOperatorExpression,
        data: DependencyNodeIndex
    ): Unit =
        booleanOperatorExpression.visit(data) {
            booleanOperatorExpression.leftOperand.visitRecursively()
            booleanOperatorExpression.rightOperand.visitRecursively()
        }

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: DependencyNodeIndex): Unit =
        checkedSafeCallSubject.visit(data) {
            checkedSafeCallSubject.originalReceiverRef.value.visitRecursively()
        }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: DependencyNodeIndex): Unit =
        checkNotNullCall.visit(data) {
            checkNotNullCall.argumentList.visitRecursively()
        }

    override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral, data: DependencyNodeIndex): Unit =
        collectionLiteral.visit(data) {
            collectionLiteral.argumentList.visitRecursively()
        }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: DependencyNodeIndex): Unit =
        comparisonExpression.visit(data) {
            comparisonExpression.compareToCall.visitRecursively()
        }

    override fun visitComponentCall(componentCall: FirComponentCall, data: DependencyNodeIndex): Unit =
        visitFunctionCall(componentCall, data)

    override fun visitDesugaredAssignmentValueReferenceExpression(
        desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression,
        data: DependencyNodeIndex
    ): Unit = desugaredAssignmentValueReferenceExpression.visit(data) {
        desugaredAssignmentValueReferenceExpression.expressionRef.value.visitRecursively()
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: DependencyNodeIndex): Unit =
        elvisExpression.visit(data) {
            elvisExpression.lhs.visitRecursively()
            elvisExpression.rhs.visitRecursively()
        }

    override fun visitEnumEntryDeserializedAccessExpression(
        enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression,
        data: DependencyNodeIndex
    ): Unit = enumEntryDeserializedAccessExpression.visit(data) {
        val enumEntry = enumEntryDeserializedAccessExpression.enumClassId.toLookupTag().toSymbol()
            ?.fullyExpandedClass()
            ?.collectEnumEntries()
            ?.find { it.name == enumEntryDeserializedAccessExpression.enumEntryName }
            ?: return@visit
        referenceNode(EnumEntryIndex(enumEntry.asEnumEntryEntity()))
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: DependencyNodeIndex): Unit =
        equalityOperatorCall.visit(data) {
            equalityOperatorCall.argumentList.visitRecursively()
        }

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: DependencyNodeIndex): Unit =
        visitFunctionCall(implicitInvokeCall, data)

    override fun visitIncrementDecrementExpression(
        incrementDecrementExpression: FirIncrementDecrementExpression,
        data: DependencyNodeIndex
    ): Unit =
        incrementDecrementExpression.visit(data) {
            incrementDecrementExpression.expression.visitRecursively()
        }

    override fun visitIntegerLiteralOperatorCall(
        integerLiteralOperatorCall: FirIntegerLiteralOperatorCall,
        data: DependencyNodeIndex
    ): Unit =
        visitFunctionCall(integerLiteralOperatorCall, data)

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: DependencyNodeIndex): Unit =
        namedArgumentExpression.visit(data) { namedArgumentExpression.expression.visitRecursively() }

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: DependencyNodeIndex): Unit =
        returnExpression.visit(data) {
            returnExpression.result.visitRecursively()
        }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: DependencyNodeIndex): Unit =
        safeCallExpression.visit(data) {
            safeCallExpression.receiver.visitRecursively()
            safeCallExpression.checkedSubjectRef.value.visitRecursively()
        }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: DependencyNodeIndex): Unit =
        smartCastExpression.visit(data) { smartCastExpression.originalExpression.visitRecursively() }

    override fun visitSpreadArgumentExpression(
        spreadArgumentExpression: FirSpreadArgumentExpression,
        data: DependencyNodeIndex
    ): Unit =
        spreadArgumentExpression.visit(data) {
            spreadArgumentExpression.expression.visitRecursively()
        }

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: DependencyNodeIndex): Unit =
        stringConcatenationCall.visit(data) {
            stringConcatenationCall.argumentList.visitRecursively()
        }

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: DependencyNodeIndex): Unit =
        throwExpression.visit(data) {
            throwExpression.exception.visitRecursively()
        }

    override fun visitTryExpression(tryExpression: FirTryExpression, data: DependencyNodeIndex): Unit =
        tryExpression.visit(data) {
            tryExpression.tryBlock.visitRecursively()
            tryExpression.catches.forEach { catch -> catch.visitRecursively() }
            tryExpression.finallyBlock?.visitRecursively()
        }

    override fun visitWhenExpression(whenExpression: FirWhenExpression, data: DependencyNodeIndex): Unit =
        whenExpression.visit(data) {
            whenExpression.subjectVariable?.visitRecursively()
            whenExpression.branches.forEach { branch -> branch.visitRecursively() }
        }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: DependencyNodeIndex): Unit =
        visitPropertyAccessExpression(whenSubjectExpression, data)

    override fun visitWhenBranch(whenBranch: FirWhenBranch, data: DependencyNodeIndex): Unit = whenBranch.visit(data) {
        whenBranch.condition.visitRecursively()
        whenBranch.result.visitRecursively()
    }

    override fun visitWrappedArgumentExpression(
        wrappedArgumentExpression: FirWrappedArgumentExpression,
        data: DependencyNodeIndex
    ): Unit = wrappedArgumentExpression.visit(data) {
        wrappedArgumentExpression.expression.visitRecursively()
    }

    override fun visitWrappedDelegateExpression(
        wrappedDelegateExpression: FirWrappedDelegateExpression,
        data: DependencyNodeIndex
    ): Unit = wrappedDelegateExpression.visit(data) {
        wrappedDelegateExpression.expression.visitRecursively()
    }

    override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: DependencyNodeIndex): Unit =
        wrappedExpression.visit(data) { wrappedExpression.expression.visitRecursively() }
}
