/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.ExpectForActualMatchingData
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

object FirExpectActualResolver {
    fun findExpectForActual(
        actualSymbol: FirBasedSymbol<*>,
        useSiteSession: FirSession,
        context: FirExpectActualMatchingContext,
    ): ExpectForActualMatchingData {
        with(context) {
            val result: Map<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>> = when (actualSymbol) {
                is FirCallableSymbol<*> -> {
                    val callableId = actualSymbol.callableId
                    val classId = callableId.classId
                    var actualContainingClass: FirRegularClassSymbol? = null
                    var expectContainingClass: FirRegularClassSymbol? = null
                    val candidates = when {
                        callableId.isLocal -> return emptyMap()
                        classId != null -> {
                            actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                                ?.fullyExpandedClass(useSiteSession)
                            expectContainingClass = actualContainingClass?.fir?.expectForActual
                                ?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
                                ?.singleOrNull() as? FirRegularClassSymbol

                            when {
                                actualSymbol is FirConstructorSymbol -> expectContainingClass?.getConstructors(expectScopeSession)
                                actualSymbol.isStatic -> expectContainingClass?.getStaticCallablesForExpectClass(actualSymbol.name)
                                else -> expectContainingClass?.getCallablesForExpectClass(actualSymbol.name)
                            }.orEmpty().filter { expectSymbol ->
                                // Don't match with private fake overrides
                                !expectSymbol.isFakeOverride(expectContainingClass) || expectSymbol.visibility != Visibilities.Private
                            }
                        }
                        else -> {
                            val transitiveDependsOn = actualSymbol.moduleData.allDependsOnDependencies
                            val scope =
                                FirPackageMemberScope(callableId.packageName, useSiteSession, useSiteSession.dependenciesSymbolProvider)
                            mutableListOf<FirCallableSymbol<*>>()
                                .apply {
                                    scope.processFunctionsByName(callableId.callableName) { add(it) }
                                    scope.processPropertiesByName(callableId.callableName) { add(it) }
                                }
                                .filter { expectSymbol -> expectSymbol.isExpect && expectSymbol.moduleData in transitiveDependsOn }
                                .filterContainedInTheFirstWaveOfDependsOnDominatorTree(graphStartingNode = actualSymbol.moduleData)
                        }
                    }
                    candidates
                        .filter { expectSymbol -> actualSymbol != expectSymbol }
                        .groupBy { expectDeclaration ->
                            AbstractExpectActualMatcher.getCallablesMatchingCompatibility(
                                expectDeclaration,
                                actualSymbol as CallableSymbolMarker,
                                expectContainingClass,
                                actualContainingClass,
                                context
                            )
                        }
                        .let {
                            // If there is a compatible entry, return a map only containing it
                            when (val compatibleSymbols = it[ExpectActualMatchingCompatibility.MatchedSuccessfully]) {
                                null -> it
                                else -> mapOf(ExpectActualMatchingCompatibility.MatchedSuccessfully to compatibleSymbols)
                            }
                        }
                }
                is FirClassLikeSymbol<*> -> {
                    val transitiveDependsOn = actualSymbol.moduleData.allDependsOnDependencies
                    transitiveDependsOn
                        .mapNotNull { it.session.symbolProvider.getClassLikeSymbolByClassId(actualSymbol.classId) }
                        .filter { it.isExpect && it.moduleData in transitiveDependsOn }
                        .filterIsInstance<FirRegularClassSymbol>()
                        .distinct()
                        .filterContainedInTheFirstWaveOfDependsOnDominatorTree(graphStartingNode = actualSymbol.moduleData)
                        .groupBy { AbstractExpectActualMatcher.matchClassifiers(expectClassSymbol = it, actualSymbol, context) }
                }
                else -> emptyMap()
            }
            return result
        }
    }
}

/**
 * [graphStartingNode] is list of modules where we should start the graph traversal from.
 * The result of the function is List of elements from the receiver
 * that are reachable from [graphStartingNode] without going through other elements from the receiver.
 * (an interesting observation that the result of the function is the same whether you run the algorithm on the graph itself or its dominator tree)
 *
 * Note: the algorithm forbids expect-actual relations that could theoretically be allowed.
 * For example:
 * ```
 *   module1     (actual class Foo)
 *   |     ↓
 *   |  module2  (expect class Foo)
 *   ↓     ↓
 *   module3     (expect class Foo)
 * ```
 *
 * The current algorithm forbids such configuration.
 *
 * ## Alternative
 *
 * An alternative less strict algorithm that should allow all reasonable configurations is:
 * 1. Filter modules to keep only those that contain expect declarations
 * 2. Expects are in "the first wave" of the topologically sorted graph
 *
 * Please note that the first step is important to ban configurations like this:
 * ```
 *      module1      (actual class Foo)
 *      ↓     |
 *   module2  ↓      (nothing in module2)
 *      ↓  module3   (expect class Foo)
 *   module4         (expect class Foo)
 * ```
 * Otherwise, `module4` won't appear in "the first wave" of the topologically sorted graph,
 * and `AMBIGUOUS_EXPECTS` won't be reported.
 */
private fun <T : FirBasedSymbol<*>> Iterable<T>.filterContainedInTheFirstWaveOfDependsOnDominatorTree(
    graphStartingNode: FirModuleData,
): List<T> {
    val modulesOfInterest: Map<FirModuleData, List<T>> = groupBy { it.moduleData }
    // In happy cases (the majority of the cases), only 1 expect declaration is available;
    // Otherwise, an `AMBIGUOUS_EXPECTS` diagnostic is reported.
    val result: MutableList<T> = ArrayList(1)
    val visited: MutableSet<FirModuleData> = HashSet()
    fun dfs(module: FirModuleData) {
        if (!visited.add(module)) return
        val data = modulesOfInterest[module]
        if (data != null) {
            // We found a node/module of interest (a module that contains an "expect" declaration in it),
            // stop there and don't visit everything that is "transitively reachable further"
            result.addAll(data)
        } else {
            for (dependency in module.dependsOnDependencies) {
                dfs(dependency)
            }
        }
    }
    dfs(graphStartingNode)
    return result
}
