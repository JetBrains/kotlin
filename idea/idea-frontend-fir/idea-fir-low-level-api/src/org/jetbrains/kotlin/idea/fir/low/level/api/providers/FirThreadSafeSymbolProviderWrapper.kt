/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.providers

import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.annotations.PrivateForInline
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

internal class FirThreadSafeSymbolProviderWrapper(private val provider: FirSymbolProvider) : FirSymbolProvider(provider.session) {
    private val lock = ReentrantReadWriteLock()
    private val classesCache = ThreadSafeCache<ClassId, FirClassLikeSymbol<*>>(lock)
    private val topLevelCache = ThreadSafeCache<CallableId, List<FirCallableSymbol<*>>>(lock)
    private val packages = ThreadSafeCache<FqName, FqName>(lock)

    override fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>? =
        classesCache.getOrCompute(classId) {
            provider.getClassLikeSymbolByFqName(classId)
        }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> =
        topLevelCache.getOrCompute(CallableId(packageFqName, name)) {
            provider.getTopLevelCallableSymbols(packageFqName, name)
        } ?: emptyList()

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        error("Should not be called for wrapper")
    }

    override fun getPackage(fqName: FqName): FqName? =
        packages.getOrCompute(fqName) { provider.getPackage(fqName) }
}

private class ThreadSafeCache<KEY, VALUE : Any>(private val lock: ReadWriteLock) {
    private val map = HashMap<KEY, Any>()

    @OptIn(PrivateForInline::class)
    inline fun getOrCompute(key: KEY, compute: () -> VALUE?): VALUE? {
        var value = lock.readLock().withLock { map[key] }
        if (value == null) {
            lock.writeLock().withLock {
                value = compute() ?: NULLABLE_VALUE
                map[key] = value!!
            }
        }
        @Suppress("UNCHECKED_CAST")
        return when (value) {
            NULLABLE_VALUE -> null
            null -> error("We should not read null from map here")
            else -> value as VALUE
        }
    }
}

@Suppress("ClassName")
@PrivateForInline
internal object NULLABLE_VALUE