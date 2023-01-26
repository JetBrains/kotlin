/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.flatMapToNullableSet
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.functionTypeService
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@NoMutableState
class FirCachingCompositeSymbolProvider(
    session: FirSession,
    val providers: List<FirSymbolProvider>,
    // This property is necessary just to make sure we don't use the hack at `createCopyWithCleanCaches` more than once or in cases
    // we are not assumed to use it.
    private val expectedCachesToBeCleanedOnce: Boolean = false,
) : FirSymbolProvider(session) {

    private val classLikeCache = session.firCachesFactory.createCache(::computeClass)
    private val topLevelCallableCache = session.firCachesFactory.createCache(::computeTopLevelCallables)
    private val topLevelFunctionCache = session.firCachesFactory.createCache(::computeTopLevelFunctions)
    private val topLevelPropertyCache = session.firCachesFactory.createCache(::computeTopLevelProperties)
    private val packageCache = session.firCachesFactory.createCache(::computePackage)

    private val callablePackageSet: Set<String>? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageSetWithTopLevelCallables().also {
            ensureNotNull(it) { "package names with callables" }
        }
    }

    private val knownTopLevelClassifierNamesInPackage: FirCache<FqName, Set<String>?, Nothing?> =
        session.firCachesFactory.createCache { packageFqName ->
            knownTopLevelClassifiersInPackage(packageFqName).also {
                ensureNotNull(it) { "classifier names in package $packageFqName" }
            }
        }

    private val callableNamesInPackage: FirCache<FqName, Set<Name>?, Nothing?> =
        session.firCachesFactory.createCache { packageFqName ->
            computeCallableNamesInPackage(packageFqName).also {
                ensureNotNull(it) { "callable names in package $packageFqName" }
            }
        }

    private inline fun ensureNotNull(v: Any?, representation: () -> String) {
        require(v != null || expectedCachesToBeCleanedOnce) {
            "${representation()} is expected to be not null in CLI"
        }
    }

    // Unfortunately, this is a part of a hack for overcoming the problem of plugin's generated entities
    // (for more details see its usage at org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveProcessor.afterPhase)
    fun createCopyWithCleanCaches(): FirCachingCompositeSymbolProvider {
        require(expectedCachesToBeCleanedOnce) { "Unexpected caches clearing" }
        return FirCachingCompositeSymbolProvider(session, providers, expectedCachesToBeCleanedOnce = false)
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        if (!mayHaveTopLevelCallablesInPackage(packageFqName, name)) return emptyList()
        return topLevelCallableCache.getValue(CallableId(packageFqName, name))
    }

    private fun mayHaveTopLevelCallablesInPackage(packageFqName: FqName, name: Name): Boolean {
        if (callablePackageSet != null && packageFqName.asString() !in callablePackageSet!!) return false
        val callableNamesInPackage = callableNamesInPackage.getValue(packageFqName) ?: return true
        return name in callableNamesInPackage
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        destination += getTopLevelCallableSymbols(packageFqName, name)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!mayHaveTopLevelCallablesInPackage(packageFqName, name)) return
        destination += topLevelFunctionCache.getValue(CallableId(packageFqName, name))
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!mayHaveTopLevelCallablesInPackage(packageFqName, name)) return
        destination += topLevelPropertyCache.getValue(CallableId(packageFqName, name))
    }

    override fun getPackage(fqName: FqName): FqName? {
        return packageCache.getValue(fqName)
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val knownClassifierNames = knownTopLevelClassifierNamesInPackage.getValue(classId.packageFqName)
        if (knownClassifierNames != null && !isNameForFunctionClass(classId)) {
            val outerClassId = classId.outerClassId
            if (outerClassId == null && classId.shortClassName.asString() !in knownClassifierNames) return null
            if (outerClassId != null && classId.outermostClassId.shortClassName.asString() !in knownClassifierNames) return null
        }

        return classLikeCache.getValue(classId)
    }

    private fun isNameForFunctionClass(classId: ClassId): Boolean {
        return session.functionTypeService.getKindByClassNamePrefix(classId.packageFqName, classId.shortClassName.asString()) != null
    }

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

    override fun computePackageSetWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.computePackageSetWithTopLevelCallables() }

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? =
        providers.flatMapToNullableSet { it.knownTopLevelClassifiersInPackage(packageFqName) }

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.computeCallableNamesInPackage(packageFqName) }
}
