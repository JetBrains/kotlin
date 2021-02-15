/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

abstract class FirCache<in K : Any, out V, in CONTEXT> {
    abstract fun getValue(key: K, context: CONTEXT): V
    abstract fun getValueIfComputed(key: K): V?
}

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Any, V> FirCache<K, V, Nothing?>.getValue(key: K): V =
    getValue(key, null)
