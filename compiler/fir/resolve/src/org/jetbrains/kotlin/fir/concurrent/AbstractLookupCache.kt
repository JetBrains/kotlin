/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

abstract class AbstractLookupCache<K : Any, V> {
    abstract fun lookup(key: K, compute: (K) -> V): V
}

abstract class AbstractLateBindingLookupCache<K : Any, V> : AbstractLookupCache<K, V>() {
    abstract fun lookup(key: K, compute: (K) -> V, postCompute: (V) -> Unit): V
    override fun lookup(key: K, compute: (K) -> V): V {
        return lookup(key, compute) {}
    }
}