/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import kotlin.reflect.KClass

/**
 * Write access ([registerComponent]/[removeComponent]) is thread **unsafe**.
 * Read access is thread **safe** only if there is no concurrent [removeComponent].
 *
 * [AttributeArrayOwner] based on different implementations of [ArrayMap] and switches them
 *   depending on array map fullness
 * [AttributeArrayOwner] can be used in classes with many instances,
 *   like user data for Fir elements or attributes for cone types
 *
 * Note that you can remove attributes from [AttributeArrayOwner] despite
 *   from components in [ComponentArrayOwner]
 */
abstract class AttributeArrayOwner<K : Any, T : Any> protected constructor(
    arrayMap: ArrayMap<T>,
) : AbstractArrayMapOwner<K, T>() {
    final override var arrayMap: ArrayMap<T> = arrayMap
        private set

    @Suppress("UNCHECKED_CAST")
    constructor() : this(EmptyArrayMap as ArrayMap<T>)

    // The map implementation is chosen by its concrete type rather than by size, because an ArrayMapImpl that has
    // shrunk (via removeComponent) can hold as few as 2 entries, so size no longer implies the implementation.
    @Suppress("UNCHECKED_CAST")
    final override fun registerComponent(keyQualifiedName: String, value: T) {
        val id = typeRegistry.getId(keyQualifiedName)
        // Every transition publishes a fresh map so concurrent readers always observe a fully-populated instance
        // (see the class documentation for the thread-safety contract). Only ArrayMapImpl is mutated in place.
        when (val map = arrayMap) {
            is EmptyArrayMap -> {
                arrayMap = OneElementArrayMap(value, id)
            }
            is OneElementArrayMap<*> -> {
                map as OneElementArrayMap<T>
                arrayMap = if (map.index == id) {
                    OneElementArrayMap(value, id)
                } else {
                    TinyArrayMap(map.index, map.value, id, value)
                }
            }
            is TinyArrayMap<*> -> {
                // TODO: Rewrite in terms of `hasKey` and move the `ArrayMapImpl` construction out of `withAddedOrReplaced`.
                arrayMap = (map as TinyArrayMap<T>).withAddedOrReplaced(id, value)
            }
            is ArrayMapImpl<*> -> {
                (map as ArrayMapImpl<T>)[id] = value
            }
//            else -> TODO()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun removeComponent(tClass: KClass<out K>) {
        val id = typeRegistry.getId(tClass)
        if (arrayMap[id] == null) return
        when (val map = arrayMap) {
            // Unreachable: an absent id returned above, so an empty map never reaches here.
            is EmptyArrayMap -> {}
            is OneElementArrayMap<*> -> arrayMap = EmptyArrayMap as ArrayMap<T>
            is TinyArrayMap<*> -> arrayMap = (map as TinyArrayMap<T>).withRemoved(id) // TODO: Downgrade outside of `withRemoved`.
            is ArrayMapImpl<*> -> {
                map as ArrayMapImpl<T>
                map.remove(id)
                if (map.size == 1) { // TODO: Maybe downgrade to tiny map after all.
                    (val index = key, val value) = map.entries().first()
                    arrayMap = OneElementArrayMap(value, index)
                }
            }
//            else -> TODO()
        }
    }
}
