/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.caches

import kotlin.reflect.KProperty

abstract class FirCache<in K : Any, out V, in CONTEXT> {
    abstract fun getValue(key: K, context: CONTEXT): V
    abstract fun getValueIfComputed(key: K): V?
}

@Suppress("NOTHING_TO_INLINE")
inline fun <K : Any, V> FirCache<K, V, Nothing?>.getValue(key: K): V =
    getValue(key, null)

operator fun <K : Any, V> FirCache<K, V, Nothing>.contains(key: K): Boolean {
    return getValueIfComputed(key) != null
}

class FirLazyValue<out V, in CONTEXT>(private val cache: FirCache<Unit, V, CONTEXT>) {
    fun getValue(context: CONTEXT): V {
        return cache.getValue(Unit, context)
    }
}

operator fun <V> FirLazyValue<V, Nothing?>.getValue(thisRef: Any?, property: KProperty<*>): V {
    return getValue(context = null)
}

@Suppress("NOTHING_TO_INLINE")
inline fun <V> FirLazyValue<V, Nothing?>.getValue(): V {
    return getValue(null)
}
