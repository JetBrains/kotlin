/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirOnlyCallablesScope
import org.jetbrains.kotlin.fir.scopes.impl.FirOnlyClassifiersScope
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

fun FirClassLikeDeclaration.fullyExpandedClass(useSiteSession: FirSession): FirRegularClass? {
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
    return when {
        classLikeSymbol != null -> {
            val classSymbol = classLikeSymbol.fir.fullyExpandedClass(useSiteSession)?.symbol ?: return null
            ClassQualifierReceiver(explicitReceiver, classSymbol, classLikeSymbol, useSiteSession, scopeSession)
        }
        else -> PackageQualifierReceiver(explicitReceiver, useSiteSession)
    }
}

abstract class QualifierReceiver(val explicitReceiver: FirResolvedQualifier) {
    abstract fun classifierScope(): FirScope?
    abstract fun callableScope(): FirScope?
}

class ClassQualifierReceiver(
    explicitReceiver: FirResolvedQualifier,
    val classSymbol: FirRegularClassSymbol,
    val originalSymbol: FirClassLikeSymbol<*>,
    val useSiteSession: FirSession,
    val scopeSession: ScopeSession
) : QualifierReceiver(explicitReceiver) {

    override fun callableScope(): FirScope? {
        val klass = classSymbol.fir
        val provider = klass.scopeProvider
        return provider.getStaticMemberScopeForCallables(klass, useSiteSession, scopeSession)
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
    override fun classifierScope(): FirScope {
        return FirOnlyClassifiersScope(scope)
    }

    override fun callableScope() = FirOnlyCallablesScope(scope)
}
