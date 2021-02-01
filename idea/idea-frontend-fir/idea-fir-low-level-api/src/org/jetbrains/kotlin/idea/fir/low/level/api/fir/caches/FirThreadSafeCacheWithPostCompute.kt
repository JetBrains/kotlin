/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.fir.caches

import org.jetbrains.kotlin.fir.caches.FirCache
import java.util.concurrent.ConcurrentHashMap

internal class FirThreadSafeCacheWithPostCompute<KEY : Any, VALUE, CONTEXT, DATA>(
    private val createValue: (KEY, CONTEXT) -> Pair<VALUE, DATA>,
    private val postCompute: (KEY, VALUE, DATA) -> Unit
) : FirCache<KEY, VALUE, CONTEXT>() {
    private val map = ConcurrentHashMap<KEY, ValueWithPostCompute<KEY, VALUE, DATA>>()

    @Suppress("UNCHECKED_CAST")
    override fun getValue(key: KEY, context: CONTEXT): VALUE =
        map.computeIfAbsent(key) {
            ValueWithPostCompute(
                key,
                calculate = { createValue(it, context) },
                postCompute = postCompute
            )
        }.getValue()

    override fun getValueIfComputed(key: KEY): VALUE? =
        map[key]?.getValueIfComputed()
}