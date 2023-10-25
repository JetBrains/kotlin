/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.ExpectForActualMatchingData
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
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
        scopeSession: ScopeSession,
        context: FirExpectActualMatchingContext,
    ): ExpectForActualMatchingData {
        with(context) {
            val result: Map<ExpectActualMatchingCompatibility, List<FirBasedSymbol<*>>> = when (actualSymbol) {
                is FirCallableSymbol<*> -> {
                    val callableId = actualSymbol.callableId
                    val classId = callableId.classId
                    var expectContainingClass: FirRegularClassSymbol? = null
                    var actualContainingClass: FirRegularClassSymbol? = null
                    val candidates = when {
                        classId != null -> {
                            expectContainingClass = useSiteSession.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)?.let {
                                it.fullyExpandedClass(it.moduleData.session)
                            }
                            actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                                ?.fullyExpandedClass(useSiteSession)

                            when (actualSymbol) {
                                is FirConstructorSymbol -> expectContainingClass?.getConstructors(scopeSession)
                                else -> expectContainingClass?.getMembersForExpectClass(actualSymbol.name)
                            }.orEmpty()
                        }
                        callableId.isLocal -> return emptyMap()
                        else -> {
                            val scope = FirPackageMemberScope(callableId.packageName, useSiteSession, useSiteSession.dependenciesSymbolProvider)
                            mutableListOf<FirCallableSymbol<*>>().apply {
                                scope.processFunctionsByName(callableId.callableName) { add(it) }
                                scope.processPropertiesByName(callableId.callableName) { add(it) }
                            }
                        }
                    }
                    candidates.filter { expectSymbol ->
                        actualSymbol != expectSymbol && expectSymbol.isExpect
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
                    val expectClassSymbol = useSiteSession.dependenciesSymbolProvider
                        .getClassLikeSymbolByClassId(actualSymbol.classId) as? FirRegularClassSymbol ?: return emptyMap()
                    val compatibility = AbstractExpectActualMatcher.matchClassifiers(expectClassSymbol, actualSymbol, context)
                    mapOf(compatibility to listOf(expectClassSymbol))
                }
                else -> emptyMap()
            }
            return result
        }
    }
}
