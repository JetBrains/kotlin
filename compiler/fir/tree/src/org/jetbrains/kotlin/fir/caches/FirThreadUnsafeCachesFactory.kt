/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

object FirThreadUnsafeCachesFactory : FirCachesFactory() {
    override fun <KEY : Any, VALUE, CONTEXT> createCache(createValue: (KEY, CONTEXT) -> VALUE): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadUnsafeCache(createValue)

    override fun <KEY : Any, VALUE, CONTEXT, DATA> createCacheWithPostCompute(
        createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
        postCompute: (KEY, VALUE, DATA) -> Unit
    ): FirCache<KEY, VALUE, CONTEXT> =
        FirThreadUnsafeCacheWithPostCompute(createValue, postCompute)
}

@Suppress("UNCHECKED_CAST")
private class FirThreadUnsafeCache<KEY : Any, VALUE, CONTEXT>(
    private val createValue: (KEY, CONTEXT) -> VALUE
) : FirCache<KEY, VALUE, CONTEXT>() {
    private val map = NullableMap<KEY, VALUE>()

    override fun getValue(key: KEY, context: CONTEXT): VALUE =
        map.getOrElse(key) {
            createValue(key, context).also { createdValue ->
                map[key] = createdValue
            }
        }

    override fun getValueIfComputed(key: KEY): VALUE? =
        map.getOrElse(key) { null as VALUE }
}


private class FirThreadUnsafeCacheWithPostCompute<KEY : Any, VALUE, CONTEXT, DATA>(
    private val createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
    private val postCompute: (KEY, VALUE, DATA) -> Unit
) : FirCache<KEY, VALUE, CONTEXT>() {
    private val map = NullableMap<KEY, VALUE>()

    override fun getValue(key: KEY, context: CONTEXT): VALUE =
        map.getOrElse(key) {
            val (createdValue, data) = createValue(key, context)
            map[key] = createdValue
            postCompute(key, createdValue, data)
            createdValue
        }


    @Suppress("UNCHECKED_CAST")
    override fun getValueIfComputed(key: KEY): VALUE? =
        map.getOrElse(key) { null as VALUE }
}