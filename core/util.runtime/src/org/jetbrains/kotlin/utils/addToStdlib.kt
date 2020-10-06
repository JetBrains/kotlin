/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.addToStdlib

import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

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

inline fun <reified T> Iterable<*>.filterIsInstanceWithChecker(additionalChecker: (T) -> Boolean): List<T> {
    val result = arrayListOf<T>()
    for (element in this) {
        if (element is T && additionalChecker(element)) {
            result += element
        }
    }
    return result
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

inline fun <reified T : Any> Any?.safeAs(): T? = this as? T
inline fun <reified T : Any> Any?.cast(): T = this as T
inline fun <reified T : Any> Any?.assertedCast(message: () -> String): T = this as? T ?: throw AssertionError(message())

fun <T : Any> constant(calculator: () -> T): T {
    val cached = constantMap[calculator]
    @Suppress("UNCHECKED_CAST")
    if (cached != null) return cached as T

    // safety check
    val fields = calculator::class.java.declaredFields.filter { it.modifiers.and(Modifier.STATIC) == 0 }
    assert(fields.isEmpty()) {
        "No fields in the passed lambda expected but ${fields.joinToString()} found"
    }

    val value = calculator()
    constantMap[calculator] = value
    return value
}

private val constantMap = ConcurrentHashMap<Function0<*>, Any>()

fun String.indexOfOrNull(char: Char, startIndex: Int = 0, ignoreCase: Boolean = false): Int? =
        indexOf(char, startIndex, ignoreCase).takeIf { it >= 0 }

fun String.lastIndexOfOrNull(char: Char, startIndex: Int = lastIndex, ignoreCase: Boolean = false): Int? =
        lastIndexOf(char, startIndex, ignoreCase).takeIf { it >= 0 }

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

inline fun <T, R : Any> Array<T>.firstNotNullResult(transform: (T) -> R?): R? {
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

inline fun <T, C : Collection<T>, O> C.ifNotEmpty(body: C.() -> O?): O? = if (isNotEmpty()) this.body() else null

inline fun <T, O> Array<out T>.ifNotEmpty(body: Array<out T>.() -> O?): O? = if (isNotEmpty()) this.body() else null

inline fun <T> measureTimeMillisWithResult(block: () -> T) : Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return Pair(System.currentTimeMillis() - start, result)
}

fun <T, C : MutableCollection<in T>> Iterable<Iterable<T>>.flattenTo(c: C): C {
    for (element in this) {
        c.addAll(element)
    }
    return c
}

inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapToNullable(destination: C, transform: (T) -> Iterable<R>?): C? {
    for (element in this) {
        val list = transform(element) ?: return null
        destination.addAll(list)
    }
    return destination
}

fun <E : Enum<E>> min(a: E, b: E): E = if (a < b) a else b
fun <E : Comparable<E>> min(a: E, b: E): E = if (a < b) a else b

inline fun <T, R> Iterable<T>.same(extractor: (T) -> R): Boolean {
    val iterator = iterator()
    val firstValue = extractor(iterator.next())
    while (iterator.hasNext()) {
        val item = iterator.next()
        val value = extractor(item)
        if (value != firstValue) {
            return false
        }
    }
    return true
}

inline fun <R> runIf(condition: Boolean, block: () -> R): R? = if (condition) block() else null

inline fun <T, R> Collection<T>.foldMap(transform: (T) -> R, operation: (R, R) -> R): R {
    val iterator = iterator()
    var result = transform(iterator.next())
    while (iterator.hasNext()) {
        result = operation(result, transform(iterator.next()))
    }
    return result
}

fun <E> MutableList<E>.trimToSize(newSize: Int) {
    subList(newSize, size).clear()
}

inline fun <K, V, VA : V> MutableMap<K, V>.getOrPut(key: K, defaultValue: (K) -> VA, postCompute: (VA) -> Unit): V {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue(key)
        put(key, answer)
        postCompute(answer)
        answer
    } else {
        value
    }
}
