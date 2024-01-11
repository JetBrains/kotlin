/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.ExpectForActualMatchingData
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.dependsOnSymbolProvider
import org.jetbrains.kotlin.fir.declarations.utils.visibility
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
                    val matchedByNamesExpectCandidates = when {
                        callableId.isLocal -> return emptyMap()
                        classId != null -> {
                            actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                                ?.fullyExpandedClass(useSiteSession)
                            expectContainingClass = actualContainingClass?.fir?.expectForActual
                                ?.get(ExpectActualMatchingCompatibility.MatchedSuccessfully)
                                ?.singleOrNull() as? FirRegularClassSymbol

                            when (actualSymbol) {
                                is FirConstructorSymbol -> expectContainingClass?.getConstructors(expectScopeSession)
                                else -> expectContainingClass?.getMembersForExpectClass(actualSymbol.name)
                            }.orEmpty()
                        }
                        else -> {
                            val scope = FirPackageMemberScope(callableId.packageName, useSiteSession, useSiteSession.dependsOnSymbolProvider)
                            mutableListOf<FirCallableSymbol<*>>().apply {
                                scope.processFunctionsByName(callableId.callableName) { add(it) }
                                scope.processPropertiesByName(callableId.callableName) { add(it) }
                            }
                        }
                    }
                    // Don't match private top-level callables. It's allowed for private top-level callables from
                    // different modules (common vs platform) to "clash" (on JVM, filenames also need to be different, but it
                    // is checked by the JVM backend)
                    //
                    // The reason why it's supported, is because K1 supported it. Probably, for the consistency sake with
                    // callables inside classifiers, it'd be better to discard such callables
                    if (actualSymbol.isPrivateTopLevel(actualContainingClass)) {
                        return emptyMap()
                    }
                    matchedByNamesExpectCandidates.filter { expectSymbol ->
                        actualSymbol != expectSymbol && !expectSymbol.isPrivateTopLevel(expectContainingClass)
                    }.groupBy { expectDeclaration ->
                        AbstractExpectActualMatcher.getCallablesMatchingCompatibility(
                            expectDeclaration,
                            actualSymbol as CallableSymbolMarker,
                            expectContainingClass,
                            actualContainingClass,
                            context
                        )
                    }.let {
                        // If there is a compatible entry, return a map only containing it
                        when (val compatibleSymbols = it[ExpectActualMatchingCompatibility.MatchedSuccessfully]) {
                            null -> it
                            else -> mapOf(ExpectActualMatchingCompatibility.MatchedSuccessfully to compatibleSymbols)
                        }
                    }
                }
                is FirClassLikeSymbol<*> -> {
                    val expectClassSymbol = useSiteSession.dependsOnSymbolProvider
                        .getClassLikeSymbolByClassId(actualSymbol.classId) as? FirRegularClassSymbol ?: return emptyMap()
                    if (expectClassSymbol.classId.asString().contains("NativeMain")) {
                        java.io.File("/home/bobko/log").appendText(
                            """
                            resolver
                                classId: ${actualSymbol.classId}
                                actual:
                                    source: ${actualSymbol.source}
                                    module: ${actualSymbol.moduleData.let { listOf(it.name, it.platform, it.unique) }.joinToString(prefix = "{", postfix = "}")}
                                expect:
                                    source: ${expectClassSymbol.source}
                                    module: ${expectClassSymbol.moduleData.let { listOf(it.name, it.platform, it.unique) }.joinToString(prefix = "{", postfix = "}")}
                        """.trimIndent() + "\n")
                    }
//                    if (expectClassSymbol.isExpect) {
                    val compatibility = AbstractExpectActualMatcher.matchClassifiers(expectClassSymbol, actualSymbol, context)
                    mapOf(compatibility to listOf(expectClassSymbol))
//                    } else {
//                        emptyMap()
//                    }
                }
                else -> emptyMap()
            }
            return result
        }
    }
}

fun FirCallableSymbol<*>.isPrivateTopLevel(containingClass: FirRegularClassSymbol?): Boolean {
    return Visibilities.isPrivate(visibility) && containingClass == null
}
