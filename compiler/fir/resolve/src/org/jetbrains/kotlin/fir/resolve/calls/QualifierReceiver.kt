/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirQualifierScope
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import java.util.ArrayDeque

class QualifierReceiver(override val explicitReceiver: FirResolvedQualifier) : AbstractExplicitReceiver<FirResolvedQualifier>() {
    private fun collectSuperTypeScopesComposedByDepth(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): List<FirScope> {
        val result = mutableListOf<FirScope>()
        val provider = klass.scopeProvider
        val levelScopes = mutableListOf<FirScope>()
        var currentDepth = 1
        val queue =
            ArrayDeque<Pair<ConeClassLikeType, Int>>()
        queue.addAll(
            lookupSuperTypes(klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession).map { it to 1 }
        )
        val visitedSymbols = mutableSetOf<FirRegularClassSymbol>()
        while (queue.isNotEmpty()) {
            val (useSiteSuperType, depth) = queue.poll()
            if (depth > currentDepth) {
                currentDepth = depth
                result += FirCompositeScope(levelScopes.toMutableList())
                levelScopes.clear()
            }
            if (useSiteSuperType is ConeClassErrorType) continue
            val superTypeSymbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession) as? FirRegularClassSymbol
                ?: continue
            if (!visitedSymbols.add(superTypeSymbol)) continue
            val superTypeScope = provider.getStaticMemberScopeForCallables(
                superTypeSymbol.fir, useSiteSession, scopeSession
            )
            if (superTypeScope != null) {
                levelScopes += superTypeScope
            }
            queue.addAll(
                lookupSuperTypes(
                    superTypeSymbol.fir, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession
                ).map { it to currentDepth + 1 }
            )
        }
        return result
    }

    private fun getClassSymbolWithCallableScopes(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): Pair<FirClassSymbol<*>?, List<FirScope>> {
        val symbol = useSiteSession.firSymbolProvider.getClassLikeSymbolByFqName(classId) ?: return null to emptyList()
        if (symbol is FirTypeAliasSymbol) {
            val expansionSymbol = symbol.fir.expandedConeType?.lookupTag?.toSymbol(useSiteSession)
            if (expansionSymbol != null) {
                return getClassSymbolWithCallableScopes(expansionSymbol.classId, useSiteSession, scopeSession)
            }
        } else {
            return (symbol as? FirClassSymbol<*>)?.let { klassSymbol ->
                val klass = klassSymbol.fir
                klassSymbol to run {
                    val result = mutableListOf<FirScope>()
                    val provider = klass.scopeProvider
                    val klassScope = provider.getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession)
                    if (klassScope != null) {
                        result += klassScope
                        if (provider is KotlinScopeProvider) return@run result
                        result += collectSuperTypeScopesComposedByDepth(klass, useSiteSession, scopeSession)
                    }
                    result
                }
            } ?: (null to emptyList())
        }

        return null to emptyList()
    }

    fun qualifierScopes(useSiteSession: FirSession, scopeSession: ScopeSession): List<FirScope> {
        val classId = explicitReceiver.classId ?: return emptyList()

        val (classSymbol, callableScopes) = getClassSymbolWithCallableScopes(classId, useSiteSession, scopeSession)
        if (classSymbol != null) {
            val klass = classSymbol.fir
            val classifierScope = if ((klass as? FirRegularClass)?.hasLazyNestedClassifiers == false) {
                nestedClassifierScope(klass)
            } else {
                useSiteSession.firSymbolProvider.getNestedClassifierScope(classId)
            }

            return when {
                classifierScope == null -> {
                    callableScopes.map { FirQualifierScope(it, null) }
                }
                callableScopes.isEmpty() -> {
                    listOf(FirQualifierScope(null, classifierScope))
                }
                else -> {
                    listOf(
                        FirQualifierScope(callableScopes.first(), classifierScope)
                    ) +
                            callableScopes.drop(1).map {
                                FirQualifierScope(it, null)
                            }
                }
            }
        }
        return emptyList()
    }

    override fun scope(useSiteSession: FirSession, scopeSession: ScopeSession): FirScope? {
        return FirCompositeScope(qualifierScopes(useSiteSession, scopeSession).toMutableList())
    }
}