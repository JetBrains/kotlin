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

import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <T: Any> T?.singletonOrEmptyList(): List<T> = if (this != null) Collections.singletonList(this) else Collections.emptyList()

fun <T> T.singletonList(): List<T> = Collections.singletonList(this)

fun <T: Any> T?.singletonOrEmptySet(): Set<T> = if (this != null) Collections.singleton(this) else Collections.emptySet()

inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

inline fun <reified T : Any> Array<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

inline fun <reified T> Sequence<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

inline fun <reified T> Iterable<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

inline fun <reified T> Array<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

inline fun <reified T : Any> Iterable<*>.lastIsInstanceOrNull(): T? {
    when (this) {
        is List<*> -> {
            for (i in this.indices.reversed()) {
                val element = this[i]
                if (element is T) return element
            }
            return null
        }

        else -> {
            return reversed().firstIsInstanceOrNull<T>()
        }
    }
}

fun <T> sequenceOfLazyValues(vararg elements: () -> T): Sequence<T> = elements.asSequence().map { it() }

fun <T1, T2> Pair<T1, T2>.swap(): Pair<T2, T1> = Pair(second, first)

fun <T: Any> T.check(predicate: (T) -> Boolean): T? = if (predicate(this)) this else null

fun <T : Any> constant(calculator: () -> T): T {
    val cached = constantMap[calculator]
    if (cached != null) return cached as T

    // safety check
    val fields = calculator.javaClass.declaredFields.filter { it.modifiers.and(Modifier.STATIC) == 0 }
    assert(fields.isEmpty()) {
        "No fields in the passed lambda expected but ${fields.joinToString()} found"
    }

    val value = calculator()
    constantMap[calculator] = value
    return value
}

private val constantMap = ConcurrentHashMap<Function0<*>, Any>()

fun String.indexOfOrNull(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int? {
    val index = indexOf(char, startIndex, ignoreCase)
    return if (index >= 0) index else null
}

fun String.lastIndexOfOrNull(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int? {
    val index = lastIndexOf(char, startIndex, ignoreCase)
    return if (index >= 0) index else null
}

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
