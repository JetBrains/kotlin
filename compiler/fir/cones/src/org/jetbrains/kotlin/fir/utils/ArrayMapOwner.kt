/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.utils

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@RequiresOptIn
annotation class Protected

@OptIn(Protected::class)
abstract class AbstractArrayMapOwner<K : Any, V : Any> {
    // TODO: make [arrayMap] protected and remove annotation after KT-19306 fix
    @get:Protected
    abstract val arrayMap: ArrayMap<V>
    protected abstract val typeRegistry: TypeRegistry<K, V>

    abstract class AbstractArrayMapAccessor<K : Any, V : Any, T : V>(
        protected val key: KClass<out K>,
        protected val id: Int
    ) {
        protected fun extractValue(thisRef: AbstractArrayMapOwner<K, V>): T? {
            @Suppress("UNCHECKED_CAST")
            return thisRef.arrayMap[id] as T?
        }
    }

    protected abstract fun registerComponent(tClass: KClass<out K>, value: V)
}

class ArrayMapAccessor<K : Any, V : Any, T : V>(
    key: KClass<out K>,
    id: Int
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(key, id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T {
        return extractValue(thisRef) ?: error("No '$key'($id) in array owner: $thisRef")
    }
}

class NullableArrayMapAccessor<K : Any, V : Any, T : V>(
    key: KClass<out K>,
    id: Int
) : AbstractArrayMapOwner.AbstractArrayMapAccessor<K, V, T>(key, id), ReadOnlyProperty<AbstractArrayMapOwner<K, V>, V?> {
    override fun getValue(thisRef: AbstractArrayMapOwner<K, V>, property: KProperty<*>): T? {
        return extractValue(thisRef)
    }
}

abstract class TypeRegistry<K : Any, V : Any> {
    private val idPerType = ConcurrentHashMap<KClass<out K>, Int>()
    private val idCounter = AtomicInteger(0)


    fun <T : V, KK : K> generateAccessor(kClass: KClass<KK>): ArrayMapAccessor<K, V, T> {
        return ArrayMapAccessor(kClass, getId(kClass))
    }

    fun <T : V, KK : K> generateNullableAccessor(kClass: KClass<KK>): NullableArrayMapAccessor<K, V, T> {
        return NullableArrayMapAccessor(kClass, getId(kClass))
    }

    fun <T : K> getId(kClass: KClass<T>): Int {
        return idPerType.computeIfAbsent(kClass) { idCounter.getAndIncrement() }
    }

    protected val indices: Collection<Int>
        get() = idPerType.values
}
