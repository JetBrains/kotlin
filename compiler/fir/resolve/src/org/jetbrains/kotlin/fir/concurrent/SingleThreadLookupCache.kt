/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

import org.jetbrains.kotlin.fir.resolve.getOrPut

class SingleThreadLookupCache<K : Any, V> : AbstractLateBindingLookupCache<K, V>() {

    val storage = mutableMapOf<K, Any>()

    override fun lookup(key: K, compute: (K) -> V, postCompute: (V) -> Unit): V {
        return unbox(
            storage.getOrPut(
                key,
                { compute(key) ?: NULL },
                { postCompute(unbox(it)) }
            )
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun unbox(v: Any): V = (if (v === NULL) null else v) as V

}

private val NULL = Any()