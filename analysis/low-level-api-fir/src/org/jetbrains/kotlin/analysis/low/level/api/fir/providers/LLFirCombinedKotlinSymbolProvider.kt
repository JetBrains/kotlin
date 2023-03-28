/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLFirSymbolProviderNameCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.getKtModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
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
import org.jetbrains.kotlin.psi.KtFile

/**
 * [LLFirCombinedKotlinSymbolProvider] combines multiple [LLFirProvider.SymbolProvider]s with the following advantages:
 *
 * - The combined symbol provider can combine the "names in package" sets built by individual providers. The name set can then be checked
 *   once instead of for each subordinate symbol provider. Because Kotlin symbol providers are ordered first in
 *   [LLFirDependenciesSymbolProvider], this check is especially fruitful.
 * - For a given class or callable ID, indices can be accessed once to get relevant PSI elements. Then the correct symbol provider(s) to
 *   call can be found out via the PSI element's [KtModule]s. This avoids the need to call every single subordinate symbol provider.
 * - A small Caffeine cache can avoid most index accesses for classes, because many names are requested multiple times, with a minor memory
 *   footprint.
 *
 * [declarationProvider] must have a scope which combines the scopes of the individual [providers].
 */
internal class LLFirCombinedKotlinSymbolProvider(
    session: FirSession,
    private val project: Project,
    private val providers: List<LLFirProvider.SymbolProvider>,
    private val declarationProvider: KotlinDeclarationProvider,
) : LLFirSelectingCombinedSymbolProvider<LLFirProvider.SymbolProvider>(session, project, providers) {
    private val symbolNameCache = object : LLFirSymbolProviderNameCache(session) {
        override fun computeClassifierNames(packageFqName: FqName): Set<String>? = buildSet {
            providers.forEach { addAll(it.knownTopLevelClassifiersInPackage(packageFqName) ?: return null) }
        }

        override fun computeCallableNames(packageFqName: FqName): Set<Name>? = buildSet {
            providers.forEach { addAll(it.computeCallableNamesInPackage(packageFqName) ?: return null) }
        }
    }

    private val classifierCache = NullableCaffeineCache<ClassId, FirClassLikeSymbol<*>> { it.maximumSize(500) }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        if (!symbolNameCache.mayHaveTopLevelClassifier(classId, mayHaveFunctionClass = false)) return null

        return classifierCache.get(classId) { computeClassLikeSymbolByClassId(it) }
    }

    @OptIn(FirSymbolProviderInternals::class)
    private fun computeClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? {
        val candidates = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)
        val (ktClass, provider) = selectFirstElementInClasspathOrder(candidates) ?: return null
        return provider.getClassLikeSymbolByClassId(classId, ktClass)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelCallableSymbolsTo(destination, callableId, callableFiles)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelFunctionSymbolsTo(destination, callableId, callableFiles)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        forEachCallableProvider(packageFqName, name) { callableId, callableFiles ->
            getTopLevelPropertySymbolsTo(destination, callableId, callableFiles)
        }
    }

    /**
     * Calls [provide] on those providers which can contribute a callable of the given name.
     */
    private fun forEachCallableProvider(
        packageFqName: FqName,
        name: Name,
        provide: LLFirProvider.SymbolProvider.(CallableId, Collection<KtFile>) -> Unit,
    ) {
        if (!symbolNameCache.mayHaveTopLevelCallable(packageFqName, name)) return

        val callableId = CallableId(packageFqName, name)

        declarationProvider.getTopLevelCallableFiles(callableId)
            .groupBy { it.getKtModule(project) }
            .forEach { (ktModule, ktFiles) ->
                // If `ktModule` cannot be found in the map, `ktFiles` cannot be processed by any of the available providers, because none
                // of them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to
                // any results for `ktFiles`.
                val provider = providersByKtModule[ktModule] ?: return@forEach
                provider.provide(callableId, ktFiles)
            }
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null

    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? =
        symbolNameCache.getTopLevelClassifierNamesInPackage(packageFqName)

    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        symbolNameCache.getTopLevelCallableNamesInPackage(packageFqName)

    companion object {
        fun merge(session: FirSession, project: Project, providers: List<LLFirProvider.SymbolProvider>): FirSymbolProvider? =
            if (providers.size > 1) {
                val combinedScope = GlobalSearchScope.union(providers.map { it.session.llFirModuleData.ktModule.contentScope })
                val declarationProvider = project.createDeclarationProvider(combinedScope)
                LLFirCombinedKotlinSymbolProvider(session, project, providers, declarationProvider)
            } else providers.singleOrNull()
    }
}
