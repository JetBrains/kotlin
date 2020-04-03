/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.KotlinScopeProvider
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import java.util.ArrayDeque


fun FirClassLikeDeclaration<*>.fullyExpandedClass(useSiteSession: FirSession): FirRegularClass? {
    if (this is FirTypeAlias) return this.expandedConeType?.lookupTag?.toSymbol(useSiteSession)?.fir?.fullyExpandedClass(useSiteSession)
    if (this is FirRegularClass) return this
    error("Not supported: $this")
}

fun createQualifierReceiver(
    explicitReceiver: FirResolvedQualifier,
    useSiteSession: FirSession,
    scopeSession: ScopeSession,
): QualifierReceiver? {

    val classLikeSymbol = explicitReceiver.symbol
    when {
        classLikeSymbol != null -> {
            val classSymbol = classLikeSymbol.fir.fullyExpandedClass(useSiteSession)?.symbol ?: return null
            return ClassQualifierReceiver(explicitReceiver, classSymbol, classLikeSymbol, useSiteSession, scopeSession)
        }
        else -> {
            return PackageQualifierReceiver(explicitReceiver, useSiteSession)
        }
    }
}

abstract class QualifierReceiver(final override val explicitReceiver: FirExpression) : AbstractExplicitReceiver<FirResolvedQualifier>() {

    abstract fun classifierScope(): FirScope?
    abstract fun callableScopes(): List<FirScope>
}

class ClassQualifierReceiver(
    explicitReceiver: FirResolvedQualifier,
    val classSymbol: FirRegularClassSymbol,
    val originalSymbol: FirClassLikeSymbol<*>,
    val useSiteSession: FirSession,
    val scopeSession: ScopeSession
) : QualifierReceiver(explicitReceiver) {


    private fun collectSuperTypeScopesComposedByDepth(
        klass: FirClass<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ): List<FirScope> {
        val result = mutableListOf<FirScope>()
        val provider = klass.scopeProvider
        val levelScopes = mutableListOf<FirScope>()
        var currentDepth = 1
        val queue =
            ArrayDeque<Pair<ConeClassLikeType, Int>>()
        queue.addAll(
            lookupSuperTypes(klass, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession).map { it to 1 },
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
                superTypeSymbol.fir, useSiteSession, scopeSession,
            )
            if (superTypeScope != null) {
                levelScopes += superTypeScope
            }
            queue.addAll(
                lookupSuperTypes(
                    superTypeSymbol.fir, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession,
                ).map { it to currentDepth + 1 },
            )
        }
        return result
    }

    private fun getCallableScopes(
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
    ): List<FirScope> {
        val klass = classSymbol.fir
        val result = mutableListOf<FirScope>()
        val provider = klass.scopeProvider
        val klassScope = provider.getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession)
        if (klassScope != null) {
            result += klassScope
            if (provider is KotlinScopeProvider) return result
            result += collectSuperTypeScopesComposedByDepth(klass, useSiteSession, scopeSession)
        }
        return result
    }

    override fun callableScopes(): List<FirScope> {
        return getCallableScopes(useSiteSession, scopeSession)
    }

    override fun classifierScope(): FirScope? {
        val klass = classSymbol.fir
        return klass.scopeProvider.getNestedClassifierScope(klass, useSiteSession, scopeSession)
    }

}


class PackageQualifierReceiver(
    explicitReceiver: FirResolvedQualifier,
    useSiteSession: FirSession
) : QualifierReceiver(explicitReceiver) {
    val scope = FirPackageMemberScope(explicitReceiver.packageFqName, useSiteSession)
    override fun classifierScope(): FirScope? {
        return FirOnlyClassifiersScope(scope)
    }

    override fun callableScopes(): List<FirScope> {
        return listOf(FirOnlyCallablesScope(scope))
    }
}
