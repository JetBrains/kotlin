/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches

import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.FirCachesFactory
import java.util.concurrent.ConcurrentHashMap

object FirThreadSafeCachesFactory : FirCachesFactory() {
    override fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadSafeCache(createValue = createValue)

    override fun <K : Any, V, CONTEXT> createCache(
        initialCapacity: Int,
        loadFactor: Float,
        createValue: (K, CONTEXT) -> V
    ): FirCache<K, V, CONTEXT> =
        FirThreadSafeCache(
            ConcurrentHashMap<K, Any>(initialCapacity, loadFactor),
            createValue
        )


    override fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadSafeCacheWithPostCompute(createValue, postCompute)
}