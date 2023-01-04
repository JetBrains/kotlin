/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@RequiresOptIn
annotation class Protected

abstract class AbstractArrayMapOwner<K : Any, V : Any> : Iterable<V> {
    protected abstract val arrayMap: ArrayMap<V>
    protected abstract val typeRegistry: TypeRegistry<K, V>

    abstract class AbstractArrayMapAccessor<K : Any, V : Any, T : V>(
        protected val id: Int
    ) {
        protected fun extractValue(thisRef: AbstractArrayMapOwner<K, V>): T? {
            @Suppress("UNCHECKED_CAST")
            return thisRef.arrayMap[id] as T?
        }
    }

    protected abstract fun registerComponent(keyQualifiedName: String, value: V)

    protected fun registerComponent(tClass: KClass<out K>, value: V) {
        registerComponent(tClass.qualifiedName!!, value)
    }

    final override fun iterator(): Iterator<V> = arrayMap.iterator()

    fun isEmpty(): Boolean = arrayMap.size == 0

    fun isNotEmpty(): Boolean = arrayMap.size != 0

    operator fun get(index: Int): V? = arrayMap[index]
}

class ArrayMapAccessor<K : Any, V : Any, T : V>(
    private val keyQualifiedName: String,
    id: Int,
    val default: T? = null
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T {
        return extractValue(thisRef)
            ?: default
            ?: error("No '$keyQualifiedName'($id) in array owner: $thisRef")
    }
}

class NullableArrayMapAccessor<K : Any, V : Any, T : V>(
    id: Int
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V?> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T? {
        return extractValue(thisRef)
    }
}

abstract class TypeRegistry<K : Any, V : Any> {
    private val idPerType = ConcurrentHashMap<String, Int>()
    private val idCounter = AtomicInteger(0)

    fun <T : V, KK : K> generateAccessor(kClass: KClass<KK>, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(kClass.qualifiedName!!, getId(kClass), default)
    }

    fun <T : V> generateAccessor(keyQualifiedName: String, default: T? = null): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(keyQualifiedName, getId(keyQualifiedName), default)
    }

    fun <T : V, KK : K> generateNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, T> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    fun <KK : K> generateAnyNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, *> {
        return NullableArrayMapAccessor(getId(kClass))
    }

    fun <T : K> getId(kClass: KClass<T>): Int {
        return getId(kClass.qualifiedName!!)
    }

    fun getId(keyQualifiedName: String): Int {
        return idPerType.customComputeIfAbsent(keyQualifiedName) { idCounter.getAndIncrement() }
    }

    /*
     * This function is needed for compatibility with JDK 6
     * ArrayMap and other infrastructure is used in KotlinType, declared in :core:descriptors module, which is
     *   compiled against JDK 6 (because it's used in kotlin-reflect, which is still compatible with Java 6)
     * So the problem is that JDK 6 does not have thread-safe computeIfAbsent for ConcurrentHashMap,
     *   and we need this method to add ability to provide thread-safe implementation by hand
     */
    abstract fun ConcurrentHashMap<String, Int>.customComputeIfAbsent(
        key: String,
        compute: (String) -> Int
    ): Int

    fun allValuesThreadUnsafeForRendering(): Map<String, Int> {
        return idPerType
    }

    protected val indices: Collection<Int>
        get() = idPerType.values
}
