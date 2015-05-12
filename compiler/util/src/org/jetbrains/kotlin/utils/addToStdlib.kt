/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.utils.addToStdlib

import java.util.Collections
import java.util.NoSuchElementException

public fun <T: Any> T?.singletonOrEmptyList(): List<T> = if (this != null) Collections.singletonList(this) else Collections.emptyList()

public fun <T: Any> T?.singletonOrEmptySet(): Set<T> = if (this != null) Collections.singleton(this) else Collections.emptySet()

public inline fun <reified T : Any> Stream<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

public inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

public inline fun <reified T : Any> Array<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

public inline fun <reified T> Stream<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

public inline fun <reified T> Iterable<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

public inline fun <reified T> Array<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

public inline fun <reified T : Any> Iterable<*>.lastIsInstanceOrNull(): T? {
    when (this) {
        is List<*> -> {
            for (i in this.indices.reversed()) {
                val element = this[i]
                if (element is T) return element
            }
            return null
        }

        else -> {
            return reverse().firstIsInstanceOrNull<T>()
        }
    }
}

public fun <T> streamOfLazyValues(vararg elements: () -> T): Stream<T> = elements.stream().map { it() }

public fun <T1, T2> Pair<T1, T2>.swap(): Pair<T2, T1> = Pair(second, first)

public fun <T: Any> T.check(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null