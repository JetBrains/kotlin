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

public fun <K, V> Iterable<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    return iterator().valuesToMap(key)
}

public fun <K, V> Iterable<K>.keysToMap(value: (K) -> V): Map<K, V> {
    return iterator().keysToMap(value)
}

public fun <K, V: Any> Iterable<K>.keysToMapExceptNulls(value: (K) -> V?): Map<K, V> {
    return iterator().keysToMapExceptNulls(value)
}

public fun <K, V> Iterator<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (v in this) {
        map[key(v)] = v
    }
    return map
}

public fun <K, V> Iterator<K>.keysToMap(value: (K) -> V): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        map[k] = value(k)
    }
    return map
}

public fun <K, V: Any> Iterator<K>.keysToMapExceptNulls(value: (K) -> V?): Map<K, V> {
    val map = LinkedHashMap<K, V>()
    for (k in this) {
        val v = value(k)
        if (v != null) {
            map[k] = v
        }
    }
    return map
}