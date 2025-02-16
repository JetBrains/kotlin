/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.getProperties
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@RequiresOptIn
annotation class FirSymbolProviderInternals

/**
 * A symbol provider provides [class symbols][FirClassLikeSymbol] and [callable symbols][FirCallableSymbol] from a specific source, such as
 * source files, libraries, or generated symbols.
 *
 * [FirSymbolProvider] is an abstract class instead of an interface by design: symbol providers are queried frequently by the compiler and
 * are often used in hot spots. The `vtable` dispatch for abstract classes is generally faster than `itable` dispatch for interfaces. While
 * that difference might be optimized away during [JVM dispatch optimizations](https://shipilev.net/blog/2015/black-magic-method-dispatch/),
 * the abstract class guarantees that we can fall back to the faster `vtable` dispatch at more complicated call sites.
 */
abstract class FirSymbolProvider(val session: FirSession) : FirSessionComponent {
    abstract val symbolNamesProvider: FirSymbolNamesProvider

    abstract fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>?

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<FirNamedFunctionSymbol> {
        return buildList { getTopLevelFunctionSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name)

    @OptIn(FirSymbolProviderInternals::class)
    open fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<FirPropertySymbol> {
        return buildList { getTopLevelPropertySymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name)

    abstract fun hasPackage(fqName: FqName): Boolean
}

private fun FirSession.getClassDeclaredMemberScope(classId: ClassId): FirScope? {
    val classSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return null
    return declaredMemberScope(classSymbol.fir, memberRequiredPhase = null)
}

fun FirSession.getClassDeclaredPropertySymbols(classId: ClassId, name: Name): List<FirVariableSymbol<*>> {
    val classMemberScope = getClassDeclaredMemberScope(classId)
    return classMemberScope?.getProperties(name).orEmpty()
}


fun FirSession.getRegularClassSymbolByClassIdFromDependencies(classId: ClassId): FirRegularClassSymbol? {
    return dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
}

fun FirSession.getRegularClassSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
    return symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
}

val FirSession.symbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor()

const val DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY: String = "org.jetbrains.kotlin.fir.resolve.providers.FirDependenciesSymbolProvider"

val FirSession.dependenciesSymbolProvider: FirSymbolProvider by FirSession.sessionComponentAccessor(
    DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
)
