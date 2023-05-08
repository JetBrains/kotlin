/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirSyntheticFunctionInterfaceProviderBase.Companion.mayBeSyntheticFunctionClassName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A [FirSymbolNamesProvider] that caches all name sets.
 */
abstract class FirCachedSymbolNamesProvider(protected val session: FirSession) : FirSymbolNamesProvider() {
    abstract fun computeTopLevelClassifierNames(packageFqName: FqName): Set<String>?
    abstract fun computePackageNamesWithTopLevelCallables(): Set<String>?
    abstract fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>?

    private val topLevelClassifierNamesByPackage =
        session.firCachesFactory.createCache(::computeTopLevelClassifierNames)

    private val topLevelCallablePackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageNamesWithTopLevelCallables()
    }

    private val topLevelCallableNamesByPackage =
        session.firCachesFactory.createCache(::computeTopLevelCallableNames)

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>? =
        topLevelClassifierNamesByPackage.getValue(packageFqName)

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? = topLevelCallablePackageNames

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelCallables()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()

        return topLevelCallableNamesByPackage.getValue(packageFqName)
    }
}

class FirDelegatingCachedSymbolNamesProvider(
    session: FirSession,
    private val delegate: FirSymbolNamesProvider,
) : FirCachedSymbolNamesProvider(session) {
    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<String>? =
        delegate.getTopLevelClassifierNamesInPackage(packageFqName)

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        delegate.getPackageNamesWithTopLevelCallables()

    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelCallableNamesInPackage(packageFqName)

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = delegate.mayHaveSyntheticFunctionTypes

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = delegate.mayHaveSyntheticFunctionType(classId)
}

open class FirCompositeCachedSymbolNamesProvider(
    session: FirSession,
    val providers: List<FirSymbolNamesProvider>,
) : FirCachedSymbolNamesProvider(session) {
    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<String>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    override fun computePackageNamesWithTopLevelCallables(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }

    override fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelCallableNamesInPackage(packageFqName) }

    override val mayHaveSyntheticFunctionTypes: Boolean = providers.any { it.mayHaveSyntheticFunctionTypes }

    @OptIn(FirSymbolProviderInternals::class)
    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean {
        if (!classId.mayBeSyntheticFunctionClassName()) return false

        // We cannot use `session`'s function type service directly, because the sessions of `providers` aren't necessarily the same as
        // `session`. So we might miss some other session's synthetic function type.
        return providers.any { it.mayHaveSyntheticFunctionType(classId) }
    }

    companion object {
        fun create(session: FirSession, providers: List<FirSymbolNamesProvider>): FirSymbolNamesProvider = when (providers.size) {
            0 -> FirEmptySymbolNamesProvider
            1 -> when (val provider = providers.single()) {
                is FirCachedSymbolNamesProvider -> provider
                else -> FirDelegatingCachedSymbolNamesProvider(session, provider)
            }
            else -> FirCompositeCachedSymbolNamesProvider(session, providers)
        }

        fun fromSymbolProviders(session: FirSession, providers: List<FirSymbolProvider>): FirSymbolNamesProvider =
            create(session, providers.map { it.symbolNamesProvider })
    }
}

/**
 * Works almost as regular flatMap, but returns a set and returns null if any lambda call returned null
 */
inline fun <T, R> Iterable<T>.flatMapToNullableSet(transform: (T) -> Iterable<R>?): Set<R>? =
    flatMapTo(mutableSetOf()) { transform(it) ?: return null }
