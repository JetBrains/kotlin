/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.resolve.providers.mayHaveTopLevelClassifier
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Caches the names of classifiers and callables contained in a package. [LLFirSymbolProviderNameCache] is used by symbol providers to abort
 * symbol finding early if the symbol name isn't contained in the symbol provider's domain.
 */
internal abstract class LLFirSymbolProviderNameCache {
    abstract fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>?

    abstract fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>?

    abstract fun mayHaveTopLevelClassifier(classId: ClassId, mayHaveFunctionClass: Boolean): Boolean

    abstract fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean
}

internal abstract class LLFirSymbolProviderNameCacheBase(
    private val firSession: FirSession
) : LLFirSymbolProviderNameCache() {
    abstract fun computeClassifierNames(packageFqName: FqName): Set<String>?
    abstract fun computeCallableNames(packageFqName: FqName): Set<Name>?

    private val topLevelClassifierNamesByPackage =
        firSession.firCachesFactory.createCache<FqName, Set<String>?>(::computeClassifierNames)

    private val topLevelCallableNamesByPackage =
        firSession.firCachesFactory.createCache<FqName, Set<Name>?>(::computeCallableNames)

    final override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>? =
        topLevelClassifierNamesByPackage.getValue(packageFqName)

    final override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        topLevelCallableNamesByPackage.getValue(packageFqName)

    final override fun mayHaveTopLevelClassifier(classId: ClassId, mayHaveFunctionClass: Boolean): Boolean {
        val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
        return names.mayHaveTopLevelClassifier(classId, firSession, mayHaveFunctionClass)
    }

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        if (name.isSpecial) return true
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
}


internal object LLFirEmptySymbolProviderNameCache : LLFirSymbolProviderNameCache() {
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>? = emptySet()
    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()
    override fun mayHaveTopLevelClassifier(classId: ClassId, mayHaveFunctionClass: Boolean): Boolean = false
    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean = false
}

internal class LLFirCompositeSymbolProviderNameCache
private constructor(
    private val caches: List<LLFirSymbolProviderNameCache>
) : LLFirSymbolProviderNameCache() {
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>? {
        return buildSet {
            for (cache in caches) {
                val names = cache.getTopLevelClassifierNamesInPackage(packageFqName) ?: return null
                addAll(names)
            }
        }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? {
        return buildSet {
            for (cache in caches) {
                val names = cache.getTopLevelCallableNamesInPackage(packageFqName) ?: return null
                addAll(names)
            }
        }
    }

    override fun mayHaveTopLevelClassifier(classId: ClassId, mayHaveFunctionClass: Boolean): Boolean {
        return caches.any { it.mayHaveTopLevelClassifier(classId, mayHaveFunctionClass) }
    }

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        return caches.any { it.mayHaveTopLevelCallable(packageFqName, name) }
    }

    companion object {
        fun create(caches: List<LLFirSymbolProviderNameCache>): LLFirSymbolProviderNameCache {
            return when (caches.size) {
                0 -> LLFirEmptySymbolProviderNameCache
                1 -> caches.single()
                else -> LLFirCompositeSymbolProviderNameCache(caches)
            }
        }
    }
}