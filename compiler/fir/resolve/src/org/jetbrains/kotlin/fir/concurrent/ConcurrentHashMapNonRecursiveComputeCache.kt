/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashMapNonRecursiveComputeCache<K : Any, V>(
    val storage: ConcurrentHashMap<K, V> = ConcurrentHashMap()
) : AbstractLookupCache<K, V>() {

    override fun lookup(key: K, compute: (K) -> V): V {
        return storage.computeIfAbsent(key, compute)
    }
}