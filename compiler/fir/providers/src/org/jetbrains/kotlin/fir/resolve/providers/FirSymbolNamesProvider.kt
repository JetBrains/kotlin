/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.flatMapToNullableSet

/**
 * [FirSymbolNamesProvider] provides information about which symbols may be provided by a [FirSymbolProvider] given the symbol's name. This
 * is usually checked before the symbol is requested with functions such as [FirSymbolProvider.getClassLikeSymbolByClassId].
 *
 * All `get*` functions in this interface have the following common contract:
 *
 *  - They return `null` in case a name set is too hard, expensive, or even impossible to compute.
 *  - They might return a strict superset of the name set, i.e. the resulting set might contain some names that do not exist in the symbol
 *    provider. In other words, these sets allow false positives, but not false negatives.
 *  - It is usually not cheap to compute such sets on each query, so their result should be cached properly (see [FirCachedSymbolNamesProvider]).
 *    [FirSymbolNamesProvider]s may choose not to cache name sets if they are only going to be used for the construction of composite symbol
 *    name caches (to avoid useless layered caches) and the `mayHaveTopLevel*` functions aren't expected to be used.
 *
 * The result of [mayHaveTopLevelClassifier] and [mayHaveTopLevelCallable] may be a false positive but cannot be a false negative.
 */
abstract class FirSymbolNamesProvider {
    /**
     * Returns the set of fully qualified package names which contain any top-level declaration within the provider's scope.
     *
     * [getPackageNames] is used as the default implementation for [getPackageNamesWithTopLevelClassifiers] and
     * [getPackageNamesWithTopLevelCallables]. It depends on the symbol names provider whether it's worth computing separate package sets
     * for classifiers and callables, or just one set containing all package names.
     */
    open fun getPackageNames(): Set<String>? = null

    /**
     * Whether the symbol names provider has a specific computation for [getPackageNamesWithTopLevelClassifiers], instead of just falling
     * back to [getPackageNames]. It *does not mean* that [getPackageNamesWithTopLevelClassifiers] returns a non-null result. Rather, it
     * means that the symbol provider makes an effort to compute specific classifier package names before falling back to [getPackageNames].
     *
     * [FirCachedSymbolNamesProvider]s rely on this property to decide whether they should compute and cache classifier package names
     * separately or fall back to the cached general package names (saving time and memory).
     *
     * The value should be constant, because it allows composite symbol providers to cache their own value.
     */
    abstract val hasSpecificClassifierPackageNamesComputation: Boolean

    /**
     * Returns the set of fully qualified package names which contain a top-level classifier declaration within the provider's scope.
     */
    open fun getPackageNamesWithTopLevelClassifiers(): Set<String>? = getPackageNames()

    /**
     * Returns the set of top-level classifier names (classes, interfaces, objects, and type aliases) inside the [packageFqName] package
     * within the provider's scope.
     *
     * All usages must take into account that the result might not include `kotlin.FunctionN` (and others for which a [FunctionTypeKind]
     * exists).
     */
    abstract fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>?

    /**
     * @see hasSpecificClassifierPackageNamesComputation
     */
    abstract val hasSpecificCallablePackageNamesComputation: Boolean

    /**
     * Returns the set of fully qualified package names which contain a top-level callable declaration within the provider's scope.
     */
    open fun getPackageNamesWithTopLevelCallables(): Set<String>? = getPackageNames()

    /**
     * Returns the set of top-level callable names (functions and properties) inside the [packageFqName] package within the provider's
     * scope.
     *
     * When implementing this function, [getPackageNamesWithTopLevelCallables] should be taken into account. Specifically, if a package name
     * is not in the set of package names with top-level callables, [getTopLevelCallableNamesInPackage] must return an empty set or `null`.
     */
    abstract fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>?

    /**
     * Whether the [FirSymbolProvider] supported by this [FirSymbolNamesProvider] may contain generated function types (see
     * [FunctionTypeKind]). Names of such function types cannot be included in [getTopLevelClassifierNamesInPackage], because they are
     * generated on demand.
     *
     * [mayHaveSyntheticFunctionTypes] only needs to be overridden together with [mayHaveSyntheticFunctionType] if the supported
     * [FirSymbolProvider] can provide generated function types. The value should be constant, which allows composite symbol providers to
     * cache the result and achieve acceptable performance.
     */
    open val mayHaveSyntheticFunctionTypes: Boolean get() = false

    /**
     * Whether [classId] is considered a generated function type within the provider's scope and session.
     */
    open fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = mayHaveSyntheticFunctionTypes

    /**
     * Checks if the provider's scope may contain a top-level classifier (class, interface, object, or type alias) with the given [classId].
     */
    open fun mayHaveTopLevelClassifier(classId: ClassId): Boolean {
        if (mayHaveSyntheticFunctionTypes && mayHaveSyntheticFunctionType(classId)) return true

        // `packageNamesWithTopLevelClassifiers` is checked in `FirCachedSymbolNamesProvider.getTopLevelClassifierNamesInPackage`. It is not
        // worth checking it in uncached situations, since building the package set is as or more expensive as just building the "names in
        // package" set.
        val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
        if (classId.outerClassId == null) {
            if (!names.mayContainTopLevelClassifier(classId.shortClassName)) return false
        } else {
            if (!names.mayContainTopLevelClassifier(classId.outermostClassId.shortClassName)) return false
        }
        return true
    }

    /**
     * Checks if the provider's scope may contain a top-level callable (function or property) called [name] inside the [packageFqName]
     * package.
     */
    open fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        // Symbol providers can potentially provide symbols for special names. Hence, special names have to be allowed.
        if (name.isSpecial) return true

        // `packageNamesWithTopLevelCallables` is checked in `FirCachedSymbolNamesProvider.getTopLevelCallableNamesInPackage`. It is not
        // worth checking it in uncached situations, since building the package set is as or more expensive as just building the "names in
        // package" set.
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
}

private fun Set<Name>.mayContainTopLevelClassifier(shortClassName: Name): Boolean {
    // Symbol providers can potentially provide symbols for special names. Hence, special names have to be allowed.
    return shortClassName.isSpecial || shortClassName in this
}

/**
 * A [FirSymbolNamesProvider] for symbol providers which can't provide any symbol name sets.
 */
object FirNullSymbolNamesProvider : FirSymbolNamesProvider() {
    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null

    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    override val mayHaveSyntheticFunctionTypes: Boolean get() = true
    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = true

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = true
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = true
}

/**
 * A [FirSymbolNamesProvider] for symbol providers which don't contain *any* symbols.
 */
object FirEmptySymbolNamesProvider : FirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String> = emptySet()

    override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    override val hasSpecificCallablePackageNamesComputation: Boolean get() = false
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = emptySet()

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean = false
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

abstract class FirSymbolNamesProviderWithoutCallables : FirSymbolNamesProvider() {
    override val hasSpecificCallablePackageNamesComputation: Boolean get() = true
    override fun getPackageNamesWithTopLevelCallables(): Set<String> = emptySet()
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

open class FirCompositeSymbolNamesProvider(val providers: List<FirSymbolNamesProvider>) : FirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? {
        return providers.flatMapToNullableSet { it.getPackageNames() }
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean = providers.any { it.hasSpecificClassifierPackageNamesComputation }

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? {
        return providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelClassifiers() }
    }

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
        return providers.flatMapToNullableSet { it.getTopLevelClassifierNamesInPackage(packageFqName) }
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean = providers.any { it.hasSpecificCallablePackageNamesComputation }

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? {
        return providers.flatMapToNullableSet { it.getPackageNamesWithTopLevelCallables() }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return providers.flatMapToNullableSet { it.getTopLevelCallableNamesInPackage(packageFqName) }
    }

    override val mayHaveSyntheticFunctionTypes: Boolean = providers.any { it.mayHaveSyntheticFunctionTypes }

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = providers.any { it.mayHaveSyntheticFunctionType(classId) }

    companion object {
        fun create(providers: List<FirSymbolNamesProvider>): FirSymbolNamesProvider = when (providers.size) {
            0 -> FirEmptySymbolNamesProvider
            1 -> providers.single()
            else -> FirCompositeSymbolNamesProvider(providers)
        }

        fun fromSymbolProviders(providers: List<FirSymbolProvider>): FirSymbolNamesProvider {
            return create(providers.map { it.symbolNamesProvider })
        }
    }
}
