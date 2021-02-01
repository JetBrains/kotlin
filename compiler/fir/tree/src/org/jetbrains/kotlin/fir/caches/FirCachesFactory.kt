/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent

abstract class FirCachesFactory : FirSessionComponent {
    /**
     * Creates a cache with returns a value by key on demand if it is computed
     * Otherwise computes the value in [createValue] and caches it for future invocations
     *
     * [FirCache.getValue] should not be called inside [createValue]
     *
     * Where:
     * [CONTEXT] -- type of value which be used to create value by [createValue]
     */
    abstract fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): FirCache<KEY, VALUE, CONTEXT>

    /**
     * Creates a cache with returns a caches value on demand if it is computed
     * Otherwise computes the value in two phases:
     *  - [createValue] -- creates values and stores [VALUE] to cache and passes [VALUE] & [DATA] to [postCompute]
     *  - [postCompute] -- performs some operations on computed value after it placed into map
     *
     * [FirCache.getValue] can be safely called in postCompute from the same thread and correct value computed by [createValue] will be returned
     * [FirCache.getValue] should not be called inside [createValue]
     *
     * Where:
     *  [CONTEXT] -- type of value which be used to create value by [createValue]
     *  [DATA] -- type of additional data which will be passed from [createValue] to [postCompute]
     */
    abstract fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): FirCache<KEY, VALUE, CONTEXT>
}

val FirSession.firCachesFactory: FirCachesFactory by FirSession.sessionComponentAccessor()

inline fun <KEY : Any, VALUE> FirCachesFactory.createCache(
    crossinline createValue: (KEY) -> VALUE,
): FirCache<KEY, VALUE, Nothing?> = createCache(
    createValue = { key, _ -> createValue(key) },
)

inline fun <KEY : Any, VALUE, CONTEXT> FirCachesFactory.createCacheWithPostCompute(
    crossinline createValue: (KEY, CONTEXT) -> VALUE,
    crossinline postCompute: (KEY, VALUE) -> Unit
): FirCache<KEY, VALUE, CONTEXT> = createCacheWithPostCompute(
    createValue = { key, context -> createValue(key, context) to null },
    postCompute = { key, value, _ -> postCompute(key, value) }
)

inline fun <KEY : Any, VALUE> FirCachesFactory.createCacheWithPostCompute(
    crossinline createValue: (KEY) -> VALUE,
    crossinline postCompute: (KEY, VALUE) -> Unit
): FirCache<KEY, VALUE, Nothing?> = createCacheWithPostCompute(
    createValue = { key, _ -> createValue(key) to null },
    postCompute = { key, value, _ -> postCompute(key, value) }
)

inline fun <KEY : Any, VALUE, DATA> FirCachesFactory.createCacheWithPostCompute(
    crossinline createValue: (KEY) -> Pair<VALUE, DATA>,
    crossinline postCompute: (KEY, VALUE, DATA) -> Unit
): FirCache<KEY, VALUE, Nothing?> = createCacheWithPostCompute(
    createValue = { key, _ -> createValue(key) },
    postCompute = { key, value, data -> postCompute(key, value, data) }
)


