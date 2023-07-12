/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.ref.SoftReference
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
 * Creates a **softly referenced** cache of values associated with [Class].
 * Values are computed using provided [compute] function.
 *
 * `null` values are not supported, though there aren't any technical limitations.
 */
internal fun <V : Any> createCache(compute: (Class<*>) -> V): CacheByClass<V> {
    return if (useClassValue) ClassValueCache(compute) else ConcurrentHashMapCache(compute)
}

/*
 * We can only cache SoftReference instances in our own classvalue to avoid classloader-based leak.
 *
 * In short, the following uncollectable cycle is possible otherwise:
 * ClassValue -> KPackageImpl.getClass() -> UrlClassloader -> all loaded classes by this CL ->
 *  -> kotlin.reflect.jvm.internal.ClassValueCache -> ClassValue
 */
@kotlin.reflect.jvm.internal.SuppressJdk6SignatureCheck
private class ComputableClassValue<V>(@JvmField val compute: (Class<*>) -> V) : ClassValue<SoftReference<V>>() {
    override fun computeValue(type: Class<*>): SoftReference<V> {
        return SoftReference(compute(type))
    }

    fun createNewCopy() = ComputableClassValue(compute)
}

@kotlin.reflect.jvm.internal.SuppressJdk6SignatureCheck
private class ClassValueCache<V>(compute: (Class<*>) -> V) : CacheByClass<V>() {

    @Volatile
    private var classValue = ComputableClassValue(compute)

    override fun get(key: Class<*>): V {
        val classValue = classValue
        classValue[key].get()?.let { return it }
        // Clean stale value if it was collected at some point
        classValue.remove(key)
        /*
         * Optimistic assumption: the value was invalidated at some point of time,
         * but now we do not have a memory pressure and can recompute the value
         */
        classValue[key].get()?.let { return it }
        // Assumption failed, do not retry to avoid any non-trivial GC-dependent loops and deliberately create a separate copy
        return classValue.compute(key)
    }

    override fun clear() {
        /*
         * ClassValue does not have a proper `clear()` method but is properly weak-referenced,
         * thus abandoning ClassValue instance will eventually clear all associated values.
         */
        classValue = classValue.createNewCopy()
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
