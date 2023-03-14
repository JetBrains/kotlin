/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.fir.FirSession
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
internal abstract class LLFirSymbolProviderNameCache(private val firSession: FirSession) {
    abstract fun computeClassifierNames(packageFqName: FqName): Set<String>?
    abstract fun computeCallableNames(packageFqName: FqName): Set<Name>?

    private val topLevelClassifierNamesByPackage =
        firSession.firCachesFactory.createCache<FqName, Set<String>?>(::computeClassifierNames)

    private val topLevelCallableNamesByPackage =
        firSession.firCachesFactory.createCache<FqName, Set<Name>?>(::computeCallableNames)

    fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String>? =
        topLevelClassifierNamesByPackage.getValue(packageFqName)

    fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        topLevelCallableNamesByPackage.getValue(packageFqName)

    fun mayHaveTopLevelClassifier(classId: ClassId, mayHaveFunctionClass: Boolean): Boolean {
        val names = getTopLevelClassifierNamesInPackage(classId.packageFqName) ?: return true
        return names.mayHaveTopLevelClassifier(classId, firSession, mayHaveFunctionClass)
    }

    fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        if (name.isSpecial) return true
        val names = getTopLevelCallableNamesInPackage(packageFqName) ?: return true
        return name in names
    }
}
