/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

object FirExpectActualResolver {
    fun findExpectForActual(
        actualSymbol: FirBasedSymbol<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        context: FirExpectActualMatchingContext,
    ): ExpectForActualData? {
        with(context) {
            val result = when (actualSymbol) {
                is FirCallableSymbol<*> -> {
                    val callableId = actualSymbol.callableId
                    val classId = callableId.classId
                    var parentSubstitutor: ConeSubstitutor? = null
                    var expectContainingClass: FirRegularClassSymbol? = null
                    var actualContainingClass: FirRegularClassSymbol? = null
                    val candidates = when {
                        classId != null -> {
                            expectContainingClass = useSiteSession.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)?.let {
                                it.fullyExpandedClass(it.moduleData.session)
                            }
                            actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                                ?.fullyExpandedClass(useSiteSession)

                            val expectTypeParameters = expectContainingClass?.typeParameterSymbols.orEmpty()
                            val actualTypeParameters = actualContainingClass
                                ?.typeParameterSymbols
                                .orEmpty()

                            parentSubstitutor = createExpectActualTypeParameterSubstitutor(
                                expectTypeParameters,
                                actualTypeParameters,
                                useSiteSession,
                            )

                            when (actualSymbol) {
                                is FirConstructorSymbol -> expectContainingClass?.getConstructors(scopeSession)
                                else -> expectContainingClass?.getMembersForExpectClass(actualSymbol.name)
                            }.orEmpty()
                        }
                        callableId.isLocal -> return null
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
                        AbstractExpectActualCompatibilityChecker.getCallablesCompatibility(
                            expectDeclaration,
                            actualSymbol as CallableSymbolMarker,
                            parentSubstitutor,
                            expectContainingClass,
                            actualContainingClass,
                            context
                        )
                    }.let {
                        // If there is a compatible entry, return a map only containing it
                        when (val compatibleSymbols = it[ExpectActualCompatibility.Compatible]) {
                            null -> it
                            else -> mapOf<ExpectActualCompatibility<FirBasedSymbol<*>>, _>(ExpectActualCompatibility.Compatible to compatibleSymbols)
                        }
                    }
                }
                is FirClassLikeSymbol<*> -> {
                    val expectClassSymbol = useSiteSession.dependenciesSymbolProvider
                        .getClassLikeSymbolByClassId(actualSymbol.classId) as? FirRegularClassSymbol ?: return null
                    val compatibility = AbstractExpectActualCompatibilityChecker.getClassifiersCompatibility(
                        expectClassSymbol,
                        actualSymbol,
                        checkClassScopesCompatibility = true,
                        context
                    )
                    mapOf(compatibility to listOf(expectClassSymbol))
                }
                else -> null
            }
            return result
        }
    }
}
