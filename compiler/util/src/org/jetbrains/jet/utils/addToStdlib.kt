/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.utils.addToStdlib

import java.util.HashMap
import java.util.Collections
import java.util.NoSuchElementException

deprecated("Replace with filterKeys when bootstrapped")
public fun <K, V> Map<K, V>.filterKeys_tmp(predicate: (K)->Boolean): Map<K, V> {
    val result = HashMap<K, V>()
    for ((k, v) in this) {
        if (predicate(k)) {
            result[k] = v
        }
    }
    return result
}

public fun <T: Any> T?.singletonOrEmptyList(): List<T> = if (this != null) Collections.singletonList(this) else Collections.emptyList()

public fun <T: Any> T?.singletonOrEmptySet(): Set<T> = if (this != null) Collections.singleton(this) else Collections.emptySet()

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T : Any> Stream<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T : Any> Array<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T> Stream<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T> Iterable<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

[suppress("NOTHING_TO_INLINE")]
public inline fun <reified T> Array<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}