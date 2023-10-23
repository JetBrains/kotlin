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
import org.jetbrains.kotlin.utils.flatMapToNullableSet

/**
 * A [FirSymbolNamesProvider] that caches all name sets.
 */
abstract class FirCachedSymbolNamesProvider(protected val session: FirSession) : FirSymbolNamesProvider() {
    abstract fun computePackageNames(): Set<String>?

    /**
     * This function is only called if [hasSpecificClassifierPackageNamesComputation] is `true`. Otherwise, the classifier package set will
     * be taken from the cached general package names to avoid building duplicate sets.
     */
    abstract fun computePackageNamesWithTopLevelClassifiers(): Set<String>?

    abstract fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>?

    /**
     * This function is only called if [hasSpecificCallablePackageNamesComputation] is `true`. Otherwise, the callable package set will be
     * taken from the cached general package names to avoid building duplicate sets.
     */
    abstract fun computePackageNamesWithTopLevelCallables(): Set<String>?

    abstract fun computeTopLevelCallableNames(packageFqName: FqName): Set<Name>?

    private val cachedPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        computePackageNames()
    }

    private val topLevelClassifierPackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // If there is no specific classifier package names computation, we can cache the general package names, instead of computing and
        // caching a duplicate of the general package names set. Without the static knowledge in
        // `hasSpecificClassifierPackageNamesComputation`, a composite or delegate cached symbol names provider cannot know if its child
        // providers may return a separate set from `getPackageNamesWithTopLevelClassifiers` or it just falls back to `getPackageNames`. So
        // it wouldn't be able to make a decision about whether to compute the specific package names set for classifiers.
        //
        // To illustrate this, consider the following example: A combined cached symbol names provider will build its `cachedPackageNames`
        // by calling `getPackageNames` on all child providers. Let's call the result **S1**. To build its `topLevelClassifierPackageNames`,
        // it calls `getPackageNamesWithTopLevelClassifiers` on all child providers. This produces a different set, which we'll call **S2**.
        // If the symbol names provider has no specific computation for classifier package names, S1 and S2 would be equal, but different
        // instances. A naive implementation would cache both S1 in `cachedPackageNames` and S2 in `topLevelClassifierPackageNames`.
        // `hasSpecificClassifierPackageNamesComputation` gives the provider enough information to avoid computing and caching S2 entirely,
        // so that S1 will also be cached in `topLevelClassifierPackageNames`.
        if (hasSpecificClassifierPackageNamesComputation) {
            computePackageNamesWithTopLevelClassifiers()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    private val topLevelClassifierNamesByPackage =
        session.firCachesFactory.createCache(::computeTopLevelClassifierNames)

    private val topLevelCallablePackageNames by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // See the comment in `topLevelClassifierPackageNames` above for reasoning about `hasSpecific*PackageNamesComputation`.
        if (hasSpecificCallablePackageNamesComputation) {
            computePackageNamesWithTopLevelCallables()?.let { return@lazy it }
        }
        cachedPackageNames
    }

    private val topLevelCallableNamesByPackage =
        session.firCachesFactory.createCache(::computeTopLevelCallableNames)

    override fun getPackageNames(): Set<String>? = cachedPackageNames

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? = topLevelClassifierPackageNames

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        val packageNames = getPackageNamesWithTopLevelClassifiers()
        if (packageNames != null && packageFqName.asString() !in packageNames) return emptySet()

        return getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName)
    }

    // This is used by the compiler `FirCachingCompositeSymbolProvider` to bypass the cache access for classifier package names, because
    // the compiler never computes this package set.
    protected fun getTopLevelClassifierNamesInPackageSkippingPackageCheck(packageFqName: FqName): Set<Name>? =
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
    override fun computePackageNames(): Set<String>? = delegate.getPackageNames()

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = delegate.hasSpecificClassifierPackageNamesComputation

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        delegate.getPackageNamesWithTopLevelClassifiers()

    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        delegate.getTopLevelClassifierNamesInPackage(packageFqName)

    override val hasSpecificCallablePackageNamesComputation: Boolean get() = delegate.hasSpecificCallablePackageNamesComputation

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
    override fun computePackageNames(): Set<String>? = providers.flatMapToNullableSet { it.getPackageNames() }

    override val hasSpecificClassifierPackageNamesComputation: Boolean = providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun computePackageNamesWithTopLevelClassifiers(): Set<String>? =
        providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }

    override fun computeTopLevelClassifierNames(packageFqName: FqName): Set<Name>? =
        providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }

    override val hasSpecificCallablePackageNamesComputation: Boolean = providers.any { it.hasSpecificCallablePackageNamesComputation }

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
