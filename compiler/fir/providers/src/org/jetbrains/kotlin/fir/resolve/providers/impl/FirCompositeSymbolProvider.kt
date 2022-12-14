/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@NoMutableState
class FirCompositeSymbolProvider(session: FirSession, providers: List<FirSymbolProvider>) : FirSymbolProvider(session) {

    val providers: List<FirSymbolProvider> = providers.flatMap {
        when (it) {
            is FirCompositeSymbolProvider -> it.providers
            is FirDependenciesSymbolProviderImpl -> it.dependencyProviders
            else -> listOf(it)
        }
    }

    private val classCache = session.firCachesFactory.createCache(::computeClass)
    private val topLevelCallableCache = session.firCachesFactory.createCache(::computeTopLevelCallables)
    private val topLevelFunctionCache = session.firCachesFactory.createCache(::computeTopLevelFunctions)
    private val topLevelPropertyCache = session.firCachesFactory.createCache(::computeTopLevelProperties)
    private val packageCache = session.firCachesFactory.createCache(::computePackage)

    private val allPackageNames by lazy {
        computePackageSet()
    }

    private val callableNames = session.firCachesFactory.createCache { fqName: FqName ->
        computeCallableNames(fqName)
    }

    private val knownClassNamesInPackage: FirCache<FqName, Set<String>, Nothing?> = session.firCachesFactory.createCache { fqName: FqName ->
        knownTopLevelClassifiers(fqName)
    }
    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!mayBeCallables(packageFqName, name)) return
        destination += topLevelFunctionCache.getValue(CallableId(packageFqName, name))
    }

    private fun mayBeCallables(fqName: FqName, name: Name): Boolean {
        if (fqName.asString() !in allPackageNames) return false
        return name in callableNames.getValue(fqName)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!mayBeCallables(packageFqName, name)) return
        destination += topLevelPropertyCache.getValue(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!mayBeCallables(packageFqName, name)) return emptyList()
        return topLevelCallableCache.getValue(CallableId(packageFqName, name))
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val parentClassId = classId.outerClassId

        if (parentClassId == null && !mayHaveTopLevelClass1(classId)) return null
        if (parentClassId != null && !mayHaveTopLevelClass1(classId.outermostClassId)) return null

        return classCache.getValue(classId)
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.getValue(fqName)
    }

    override fun computePackageSet(): Set<String> = providers.flatMapTo(mutableSetOf()) { it.computePackageSet() }

    override fun mayHaveTopLevelClass(classId: ClassId) = providers.any { it.mayHaveTopLevelClass(classId) }


    private fun mayHaveTopLevelClass1(classId: ClassId): Boolean  {
        if (FunctionClassKind.byClassNamePrefix(classId.packageFqName, classId.shortClassName.asString()) != null) return true

        return classId.shortClassName.asString() in knownClassNamesInPackage.getValue(classId.packageFqName)
    }


    override fun knownTopLevelClassifiers(fqName: FqName): Set<String> = providers.flatMapTo(mutableSetOf()) { it.knownTopLevelClassifiers(fqName) }

    override fun computeCallableNames(fqName: FqName): Set<Name> =
        providers.flatMapTo(mutableSetOf()) { it.computeCallableNames(fqName)!! }

    @OptIn(FirSymbolProviderInternals::class)
    private fun computeTopLevelCallables(callableId: CallableId): List<FirCallableSymbol<*>> = buildList {
        providers.forEach { it.getTopLevelCallableSymbolsTo(this, callableId.packageName, callableId.callableName) }
    }

    @OptIn(FirSymbolProviderInternals::class)
    private fun computeTopLevelFunctions(callableId: CallableId): List<FirNamedFunctionSymbol> = buildList {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(this, callableId.packageName, callableId.callableName) }
    }

    @OptIn(FirSymbolProviderInternals::class)
    private fun computeTopLevelProperties(callableId: CallableId): List<FirPropertySymbol> = buildList {
        providers.forEach { it.getTopLevelPropertySymbolsTo(this, callableId.packageName, callableId.callableName) }
    }

    private fun computePackage(it: FqName): FqName? =
        providers.firstNotNullOfOrNull { provider -> provider.getPackage(it) }

    private fun computeClass(classId: ClassId): FirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { provider -> provider.getClassLikeSymbolByClassId(classId) }
}
