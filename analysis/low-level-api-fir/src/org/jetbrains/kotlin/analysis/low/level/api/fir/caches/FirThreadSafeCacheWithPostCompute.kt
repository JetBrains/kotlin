/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import org.jetbrains.kotlin.fir.caches.FirCache
import java.util.concurrent.ConcurrentHashMap

class FirThreadSafeCacheWithPostCompute<K : Any, V, CONTEXT, DATA>(
    val createValue: (K, CONTEXT) -> Pair<V, DATA>,
    val postCompute: (K, V, DATA) -> Unit
) : FirCache<K, V, CONTEXT>() {
    val map = ConcurrentHashMap<K, ValueWithPostCompute<K, V, DATA>>()

    override fun getValue(key: K, context: CONTEXT): V =
        map.getOrPut(key) {
            ValueWithPostCompute(
                key,
                calculate = { createValue(it, context) },
                postCompute = postCompute
            )
        }.getValue()

    override fun getValueIfComputed(key: K): V? =
        map[key]?.getValueIfComputed()
}
