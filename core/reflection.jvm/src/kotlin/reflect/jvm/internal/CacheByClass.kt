/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.util.concurrent.ConcurrentHashMap

/*
 * By default, we use ClassValue-based caches in reflection to avoid classloader leaks,
 * but ClassValue is not available on Android, thus we attempt to check it dynamically
 * and fallback to ConcurrentHashMap-based cache.
 *
 * NB: if you are changing the name of the outer file (CacheByClass.kt), please also change the corresponding
 * proguard rules
 */
private val useClassValue = runCatching {
    Class.forName("java.lang.ClassValue")
}.map { true }.getOrDefault(false)

internal abstract class CacheByClass<V> {
    abstract fun get(key: Class<*>): V

    abstract fun clear()
}

/**
 * Creates a **strongly referenced** cache of values associated with [Class].
 * Values are computed using provided [compute] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal fun <V : Any> createCache(compute: (Class<*>) -> V): CacheByClass<V> {
    return if (useClassValue) ClassValueCache(compute) else ConcurrentHashMapCache(compute)
}

private class ClassValueCache<V>(private val compute: (Class<*>) -> V) : CacheByClass<V>() {

    @Volatile
    private var classValue = initClassValue()

    private fun initClassValue() = object : ClassValue<V>() {
        override fun computeValue(type: Class<*>): V {
            return compute(type)
        }
    }

    override fun get(key: Class<*>): V = classValue[key]

    override fun clear() {
        /*
         * ClassValue does not have a proper `clear()` method but is properly weak-referenced,
         * thus abandoning ClassValue instance will eventually clear all associated values.
         */
        classValue = initClassValue()
    }
}

/**
 * We no longer support Java 6, so the only place we use this cache is Android, where there
 * are no classloader leaks issue, thus we can safely use strong references and do not bother
 * with WeakReference wrapping.
 */
private class ConcurrentHashMapCache<V>(private val compute: (Class<*>) -> V) : CacheByClass<V>() {
    private val cache = ConcurrentHashMap<Class<*>, V>()

    override fun get(key: Class<*>): V = cache.getOrPut(key) { compute(key) }

    override fun clear() {
        cache.clear()
    }
}
