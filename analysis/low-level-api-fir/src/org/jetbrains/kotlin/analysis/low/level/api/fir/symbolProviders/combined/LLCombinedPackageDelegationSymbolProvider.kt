/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize

/**
 * [LLCombinedPackageDelegationSymbolProvider] combines multiple [FirSymbolProvider]s by delegating to the appropriate individual providers
 * based on package names.
 *
 * Unlike [LLCombinedKotlinSymbolProvider], which delegates based on the modules of index-provided candidates, this provider builds a map
 * from package [FqName]s to lists of symbol providers that can provide symbols for that package. Then, for each symbol request, it queries
 * only the relevant providers for the given package.
 *
 * If any provider's [FirSymbolNamesProvider.getPackageNames] returns `null`, [LLCombinedPackageDelegationSymbolProvider] cannot be used. In
 * that case, [merge] falls back to [FirCompositeSymbolProvider], which queries all providers individually.
 *
 * The symbol provider affords its simple implementation due to the following factors:
 *
 * - There is no need to check `mayHaveTopLevel*` functions as the access to [providersByPackage] already covers the package name check: If
 *   a package cannot be provided by the combined symbol provider, the map will contain no entry for it. The callable name check itself is
 *   covered on the individual symbol provider level.
 * - There is no need for caches since the delegation is simple.
 * - [providersByPackage] is built in classpath order of the providers, so the symbol provider lists will reflect the classpath order,
 *   preserving it without a need for additional priority logic.
 *
 * ### Usage Notes
 *
 * [LLCombinedPackageDelegationSymbolProvider] is a better choice for combining library symbol providers than
 * [LLCombinedKotlinSymbolProvider]. This is because of the different lifetimes of library symbol providers and combined symbol providers:
 *
 * - Individual library symbol providers are invalidated infrequently, as library sessions outlast source sessions, so they generally have
 *   symbols already cached.
 * - Combined symbol providers are part of use-site source sessions, so they are invalidated more frequently.
 *
 * [LLCombinedKotlinSymbolProvider] accesses the index before calling into individual symbol providers. Using this symbol provider to
 * combine library symbol providers led to the following situation: As source sessions were invalidated, [LLCombinedKotlinSymbolProvider]
 * had to redo index accesses which would not have been performed by individual library symbol providers. This led to an overall performance
 * degradation in some cases.
 *
 * On the flipside, [LLCombinedPackageDelegationSymbolProvider] is not automatically better than [LLCombinedKotlinSymbolProvider]. First,
 * package delegation requires that all individual symbol providers can compute package sets, which currently isn't the case for source
 * symbol providers. But [LLCombinedKotlinSymbolProvider] might also be better in cases where multiple index accesses would be performed in
 * individual symbol providers, whereas [LLCombinedKotlinSymbolProvider] can perform a single index access.
 *
 * @param providersByPackage For the implementation of [hasPackage], [providersByPackage] must contain a transitive closure of all parent
 *  packages. For example, if the map contains 'foo.bar.baz', it must also contain 'foo.bar' and 'foo'. The entry for 'foo.bar' must contain
 *  all providers that have any package matching the prefix 'foo.bar*'.
 *
 *  [providersByPackage] has [String] keys instead of [FqName] keys to conserve memory. Furthermore, it uses arrays for the same purpose.
 */
internal class LLCombinedPackageDelegationSymbolProvider private constructor(
    session: FirSession,
    override val providers: List<FirSymbolProvider>,
    private val providersByPackage: Map<String, Array<FirSymbolProvider>>
) : LLCombinedSymbolProvider<FirSymbolProvider>(session) {
    override val symbolNamesProvider: FirSymbolNamesProvider = FirCompositeCachedSymbolNamesProvider.fromSymbolProviders(session, providers)

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val relevantProviders = providersByPackage[classId.packageFqName.asString()] ?: return null

        return relevantProviders.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val relevantProviders = providersByPackage[packageFqName.asString()] ?: return

        relevantProviders.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        val relevantProviders = providersByPackage[fqName.asString()] ?: return false

        // We still have to query individual providers since the package sets from symbol names providers may contain false positives, so
        // the `fqName` being in `providersByPackage` doesn't prove that the package exists.
        return relevantProviders.any { it.hasPackage(fqName) }
    }

    override fun estimateSymbolCacheSize(): Long = 0

    companion object {
        fun merge(session: FirSession, providers: List<FirSymbolProvider>): FirSymbolProvider? =
            if (providers.size > 1) {
                val providersByPackage = buildPackageToProvidersMap(providers)
                if (providersByPackage != null) {
                    LLCombinedPackageDelegationSymbolProvider(session, providers, providersByPackage)
                } else {
                    FirCompositeSymbolProvider(session, providers)
                }
            } else providers.singleOrNull()

        /**
         * Builds the "package to providers" map. If any package set is `null`, the resulting map will be `null` as well, and we'll need to
         * fall back to querying all providers individually.
         */
        private fun buildPackageToProvidersMap(providers: List<FirSymbolProvider>): Map<String, Array<FirSymbolProvider>>? {
            val providerListsByPackage = buildMap {
                providers.forEach { provider ->
                    val packageNames = provider.symbolNamesProvider.getPackageNames() ?: return null
                    packageNames.forEach { packageName ->
                        // We only use the `FqName` here for convenience. It won't be stored.
                        FqName(packageName).forEachFqName { fqName ->
                            val list = getOrPut(fqName.asString()) { mutableListOf() }

                            // Parent package names may overlap (e.g. 'foo.bar' and 'foo.baz' have the same parent 'foo'), so we have to be
                            // careful not to add the same provider multiple times. Since we're iterating provider by provider, the current
                            // provider will always be last in the list if it has been added, so we just need to check the last element.
                            if (list.lastOrNull() != provider) {
                                list.add(provider)
                            }
                        }
                    }
                }
            }

            // Avoid linked hash maps to conserve memory.
            return newHashMapWithExpectedSize<String, Array<FirSymbolProvider>>(providerListsByPackage.size).apply {
                providerListsByPackage.forEach { (packageName, providers) ->
                    this[packageName] = providers.toTypedArray()
                }
            }
        }

        private inline fun FqName.forEachFqName(f: (FqName) -> Unit) {
            var current: FqName? = this
            while (current != null) {
                f(current)
                current = current.parentOrNull()
            }
        }
    }
}
