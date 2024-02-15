/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.name.Name

abstract class AbstractFirUseSiteMemberScope(
    val ownerClassLookupTag: ConeClassLikeLookupTag,
    session: FirSession,
    overrideCheckerForBaseClass: FirOverrideChecker,
    // null means "use overrideCheckerForBaseClass"
    overrideCheckerForIntersection: FirOverrideChecker?,
    protected val superTypeScopes: List<FirTypeScope>,
    dispatchReceiverType: ConeSimpleKotlinType,
    protected val declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirOverrideScope(session, overrideCheckerForBaseClass) {
    protected val supertypeScopeContext = FirTypeIntersectionScopeContext(
        session,
        overrideCheckerForIntersection ?: overrideCheckerForBaseClass,
        superTypeScopes, dispatchReceiverType, forClassUseSiteScope = true
    )

    private val functions: MutableMap<Name, Collection<FirNamedFunctionSymbol>> = hashMapOf()

    private val properties: MutableMap<Name, Collection<FirVariableSymbol<*>>> = hashMapOf()
    protected val directOverriddenFunctions: MutableMap<FirNamedFunctionSymbol, List<ResultOfIntersection<FirNamedFunctionSymbol>>> =
        hashMapOf()
    protected val directOverriddenProperties: MutableMap<FirPropertySymbol, List<ResultOfIntersection<FirPropertySymbol>>> = hashMapOf()

    protected val functionsFromSupertypes: MutableMap<Name, List<ResultOfIntersection<FirNamedFunctionSymbol>>> = mutableMapOf()
    protected val propertiesFromSupertypes: MutableMap<Name, List<ResultOfIntersection<FirPropertySymbol>>> = mutableMapOf()
    protected val fieldsFromSupertypes: MutableMap<Name, List<FirFieldSymbol>> = mutableMapOf()

    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredMemberScope.getCallableNames())
            superTypeScopes.flatMapTo(this) { it.getCallableNames() }
        }
    }

    private val classifierNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredMemberScope.getClassifierNames())
            superTypeScopes.flatMapTo(this) { it.getClassifierNames() }
        }
    }

    final override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        // Important optimization: avoid creating cache keys for names that are definitely absent
        if (name !in getCallableNames()) return
        functions.getOrPut(name) {
            collectFunctions(name)
        }.forEach {
            processor(it)
        }
    }

    protected open fun collectFunctions(
        name: Name
    ): Collection<FirNamedFunctionSymbol> = mutableListOf<FirNamedFunctionSymbol>().apply {
        collectDeclaredFunctions(name, this)
        val explicitlyDeclaredFunctions = this.toSet()
        collectFunctionsFromSupertypes(name, this, explicitlyDeclaredFunctions)
    }

    protected fun collectDeclaredFunctions(name: Name, destination: MutableList<FirNamedFunctionSymbol>) {
        declaredMemberScope.processFunctionsByName(name) { symbol ->
            if (symbol.isStatic) return@processFunctionsByName
            if (!symbol.isVisibleInCurrentClass()) return@processFunctionsByName
            val directOverridden = computeDirectOverriddenForDeclaredFunction(symbol)
            directOverriddenFunctions[symbol] = directOverridden
            destination += symbol.replaceWithWrapperSymbolIfNeeded()
        }
    }

    protected abstract fun FirNamedFunctionSymbol.isVisibleInCurrentClass(): Boolean

    private fun FirCallableSymbol<*>.isInvisible(): Boolean {
        return this is FirNamedFunctionSymbol && !isVisibleInCurrentClass()
    }

    protected fun collectFunctionsFromSupertypes(
        name: Name,
        destination: MutableList<FirNamedFunctionSymbol>,
        explicitlyDeclaredFunctions: Set<FirNamedFunctionSymbol>
    ) {
        for (resultOfIntersection in getFunctionsFromSupertypesByName(name)) {
            resultOfIntersection.collectNonOverriddenDeclarations(explicitlyDeclaredFunctions, destination)
        }
    }

    /**
     * If the receiver [ResultOfIntersection] is not overridden by any symbol in [explicitlyDeclared],
     * adds its [ResultOfIntersection.chosenSymbol] to [destination].
     *
     * If the [ResultOfIntersection] is [ResultOfIntersection.NonTrivial] and only some of the intersected symbols are overridden,
     * constructs a new [ResultOfIntersection] consisting of the non-overridden symbols and adds its [ResultOfIntersection.chosenSymbol]
     * to [destination].
     *
     * It's the opposite operation of [collectDirectOverriddenForDeclared].
     */
    protected fun <T : FirCallableSymbol<*>> ResultOfIntersection<T>.collectNonOverriddenDeclarations(
        explicitlyDeclared: Set<FirCallableSymbol<*>>,
        destination: MutableList<in T>,
    ) {
        when (this) {
            is ResultOfIntersection.SingleMember -> {
                val chosenSymbol = chosenSymbol
                if (chosenSymbol.isInvisible()) return
                val overriddenBy = chosenSymbol.getOverridden(explicitlyDeclared)
                if (overriddenBy == null) {
                    destination += chosenSymbol
                }
            }
            is ResultOfIntersection.NonTrivial -> {
                // For non-trivial intersections, some of the intersected symbols can be overridden and some not.
                val (visibleNotOverridden, overriddenOrInvisible) = overriddenMembers
                    .partition { !it.member.isInvisible() && it.member.getOverridden(explicitlyDeclared) == null }

                if (overriddenOrInvisible.isEmpty()) {
                    // Case 1: all intersected symbols are overridden.
                    destination += chosenSymbol
                } else if (visibleNotOverridden.isNotEmpty()) {
                    // Case 2: some intersected symbols are overridden.
                    // Create a new ResultOfIntersection from the non-overridden and add it to destination.
                    destination += supertypeScopeContext
                        .convertGroupedCallablesToIntersectionResults(visibleNotOverridden.map { it.baseScope to listOf(it.member) })
                        .map { it.chosenSymbol }
                }
                // Case 3: all are overridden. Don't add anything to destination.
            }
        }
    }

    private fun getFunctionsFromSupertypesByName(name: Name): List<ResultOfIntersection<FirNamedFunctionSymbol>> {
        return functionsFromSupertypes.getOrPut(name) {
            supertypeScopeContext.collectIntersectionResultsForCallables(name, FirScope::processFunctionsByName)
        }
    }

    final override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        // Important optimization: avoid creating cache keys for names that are definitely absent
        if (name !in getCallableNames()) return
        properties.getOrPut(name) {
            collectProperties(name)
        }.forEach {
            processor(it)
        }
    }

    protected abstract fun collectProperties(name: Name): Collection<FirVariableSymbol<*>>

    private fun computeDirectOverriddenForDeclaredFunction(declaredFunctionSymbol: FirNamedFunctionSymbol): List<ResultOfIntersection<FirNamedFunctionSymbol>> {
        val result = mutableListOf<ResultOfIntersection<FirNamedFunctionSymbol>>()
        for (resultOfIntersection in getFunctionsFromSupertypesByName(declaredFunctionSymbol.name)) {
            resultOfIntersection.collectDirectOverriddenForDeclared(declaredFunctionSymbol, result, overrideChecker::isOverriddenFunction)
        }
        return result
    }

    /**
     * If [declared] overrides the receiver [ResultOfIntersection], adds it to [result].
     * If the [ResultOfIntersection] is [ResultOfIntersection.NonTrivial] and [declared] only overrides some of the intersected symbols,
     * a new [ResultOfIntersection] is constructed containing only the overridden symbols.
     *
     * Opposite operation to [collectNonOverriddenDeclarations].
     */
    protected inline fun <T : FirCallableSymbol<*>> ResultOfIntersection<T>.collectDirectOverriddenForDeclared(
        declared: T,
        result: MutableList<in ResultOfIntersection<T>>,
        isOverridden: (T, T) -> Boolean,
    ) {
        when (this) {
            is ResultOfIntersection.SingleMember -> {
                val symbolFromSupertype = chosenSymbol
                if (isOverridden(declared, symbolFromSupertype)) {
                    result.add(this)
                }
            }
            is ResultOfIntersection.NonTrivial -> {
                // For non-trivial intersections, declared can override a subset of the intersected symbols.
                val (overridden, nonOverridden) = overriddenMembers.partition {
                    isOverridden(declared, it.member)
                }

                if (nonOverridden.isEmpty()) {
                    // Case 1: all intersected symbols are overridden
                    result += this
                } else if (overridden.isNotEmpty()) {
                    // Case 2: some intersected symbols are overridden.
                    // Create a new ResultOfIntersection from the overridden symbols and add it to result.
                    result += supertypeScopeContext.convertGroupedCallablesToIntersectionResults(overridden.map { it.baseScope to listOf(it.member) })
                }
                // Case 3: No intersected symbols are overridden. Don't add anything to result.
            }
        }
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirNamedFunctionSymbol,
        processor: (FirNamedFunctionSymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenMembersWithBaseScopeImpl(
            directOverriddenFunctions,
            functionsFromSupertypes,
            functionSymbol,
            processor
        )
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        return processDirectOverriddenMembersWithBaseScopeImpl(
            directOverriddenProperties,
            propertiesFromSupertypes,
            propertySymbol,
            processor
        )
    }

    private fun <D : FirCallableSymbol<*>> processDirectOverriddenMembersWithBaseScopeImpl(
        directOverriddenMap: Map<D, List<ResultOfIntersection<D>>>,
        callablesFromSupertypes: Map<Name, List<ResultOfIntersection<D>>>,
        callableSymbol: D,
        processor: (D, FirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        when (val directOverridden = directOverriddenMap[callableSymbol]) {
            null -> {
                val resultOfIntersection = callablesFromSupertypes[callableSymbol.name]
                    ?.firstOrNull { it.chosenSymbol == callableSymbol }
                    ?: return ProcessorAction.NONE
                for ((overridden, baseScope) in resultOfIntersection.overriddenMembers) {
                    if (!processor(overridden, baseScope)) return ProcessorAction.STOP
                }
                return ProcessorAction.NONE
            }
            else -> {
                for (resultOfIntersection in directOverridden) {
                    for ((overridden, baseScope) in resultOfIntersection.overriddenMembers) {
                        if (!processor(overridden, baseScope)) return ProcessorAction.STOP
                    }
                }
                return ProcessorAction.NONE
            }
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        // Important optimization: avoid creating cache keys for names that are definitely absent
        if (name !in getClassifierNames()) return

        var shadowed = false
        declaredMemberScope.processClassifiersByNameWithSubstitution(name) { classifier, substitutor ->
            shadowed = true
            processor(classifier, substitutor)
        }
        if (!shadowed) {
            supertypeScopeContext.processClassifiersByNameWithSubstitution(name, processor)
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        declaredMemberScope.processDeclaredConstructors(processor)
    }

    override fun getCallableNames(): Set<Name> {
        return callableNamesCached
    }

    override fun getClassifierNames(): Set<Name> {
        return classifierNamesCached
    }

    /**
     * This function is currently used only for creating suspend views in Java.
     */
    protected open fun FirNamedFunctionSymbol.replaceWithWrapperSymbolIfNeeded(): FirNamedFunctionSymbol {
        return this
    }
}
