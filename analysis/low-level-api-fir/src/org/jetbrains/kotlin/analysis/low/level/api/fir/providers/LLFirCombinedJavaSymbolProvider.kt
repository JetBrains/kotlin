/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.createJavaClassFinder
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * [LLFirCombinedJavaSymbolProvider] combines multiple [JavaSymbolProvider]s with the following advantages:
 *
 * - For a given class ID, indices can be accessed once to get relevant PSI classes. Then the correct symbol provider(s) to call can be
 *   found out via the PSI element's [KtModule]s. This avoids the need to call every single subordinate symbol provider.
 * - A small Caffeine cache can avoid most index accesses, because many names are requested multiple times, with a minor memory footprint.
 *
 * [javaClassFinder] must have a scope which combines the scopes of the individual [providers].
 */
internal class LLFirCombinedJavaSymbolProvider private constructor(
    session: FirSession,
    project: Project,
    providers: List<JavaSymbolProvider>,
    private val javaClassFinder: JavaClassFinder,
) : LLFirSelectingCombinedSymbolProvider<JavaSymbolProvider>(session, project, providers) {
    /**
     * The purpose of this cache is to avoid index access for frequently accessed `ClassId`s, including failures. Because Java symbol
     * providers currently cannot benefit from a "name in package" check (see KTIJ-24642), the cache should also store negative results.
     *
     * The cache size has been chosen with the help of local benchmarks and performance tests. A cache size of 2500 in comparison to 1000
     * resulted in less time spent in [computeClassLikeSymbolByClassId] in local benchmarks. Cache sizes of 5000 and 10000 were tried in
     * performance tests, but didn't affect performance. A cache size of 2500 is a good middle ground with a small memory footprint.
     */
    private val classCache: NullableCaffeineCache<ClassId, FirRegularClassSymbol> = NullableCaffeineCache { it.maximumSize(2500) }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirClassLikeSymbol<*>? =
        classCache.get(classId) { computeClassLikeSymbolByClassId(it) }

    private fun computeClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        val javaClasses = javaClassFinder.findClasses(classId)
            .filterNot { javaClass -> javaClass.annotations.any { it.isResolvedTo(JvmAnnotationNames.METADATA_FQ_NAME) } }
        if (javaClasses.isEmpty()) return null

        val (javaClass, provider) = selectFirstElementInClasspathOrder(javaClasses) { javaClass ->
            // `JavaClass` doesn't know anything about PSI, but we can be sure that `findClasses` returns a `JavaClassImpl` because it's
            // using `KotlinJavaPsiFacade`. The alternative to this hack would be to change the interface of either `JavaClass` (yet the
            // module should hardly depend on PSI), or to have `KotlinJavaPsiFacade` and `JavaClassFinderImpl` return `JavaClassImpl` and to
            // return `JavaClassFinderImpl` from `createJavaClassFinder`.
            check(javaClass is JavaClassImpl) { "`findClasses` as used here should return `JavaClassImpl` results." }
            javaClass.psi
        } ?: return null

        return provider.getClassLikeSymbolByClassId(classId, javaClass)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    override fun getPackage(fqName: FqName): FqName? = providers.firstNotNullOfOrNull { it.getPackage(fqName) }

    override fun computePackageSetWithTopLevelCallables(): Set<String>? = null
    override fun knownTopLevelClassifiersInPackage(packageFqName: FqName): Set<String>? = null
    override fun computeCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null

    companion object {
        fun merge(session: FirSession, project: Project, providers: List<JavaSymbolProvider>): FirSymbolProvider? =
            if (providers.size > 1) {
                val combinedScope = GlobalSearchScope.union(providers.map { it.session.llFirModuleData.ktModule.contentScope })
                val javaClassFinder = project.createJavaClassFinder(combinedScope)
                LLFirCombinedJavaSymbolProvider(session, project, providers, javaClassFinder)
            } else providers.singleOrNull()
    }
}
