/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.utils

import java.util.LinkedHashMap
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

public fun <K, V> Stream<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (v in this) {
        map[key(v)] = v
    }
    return map
}

public fun <K, V> Stream<K>.keysToMap(value: (K) -> V): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        map[k] = value(k)
    }
    return map
}

public fun <K, V: Any> Stream<K>.keysToMapExceptNulls(value: (K) -> V?): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        val v = value(k)
        if (v != null) {
            map[k] = v
        }
    }
    return map
}

public fun <K, V> Iterable<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (v in this) {
        map[key(v)] = v
    }
    return map
}

public fun <K, V> Iterable<K>.keysToMap(value: (K) -> V): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        map[k] = value(k)
    }
    return map
}

public fun <K, V: Any> Iterable<K>.keysToMapExceptNulls(value: (K) -> V?): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        val v = value(k)
        if (v != null) {
            map[k] = v
        }
    }
    return map
}

public fun <T, C: Collection<T>> C.ifEmpty(body: () -> C): C = if (isEmpty()) body() else this

public fun <T> Iterable<Iterable<T>>.flatten(): List<T> {
    return flatMapTo(ArrayList<T>(), {it})
}

public fun <T: Any> emptyOrSingletonList(item: T?): List<T> = if (item == null) listOf() else listOf(item)

public fun <T: Any> MutableCollection<T>.addIfNotNull(t: T?) {
    if (t != null) add(t)
}

public fun <K, V> newHashMapWithExpectedSize(expectedSize: Int): HashMap<K, V> {
    return HashMap(if (expectedSize < 3) 3 else expectedSize + expectedSize / 3 + 1)
}

public fun <E> newHashSetWithExpectedSize(expectedSize: Int): HashSet<E> {
    return HashSet(if (expectedSize < 3) 3 else expectedSize + expectedSize / 3 + 1)
}
