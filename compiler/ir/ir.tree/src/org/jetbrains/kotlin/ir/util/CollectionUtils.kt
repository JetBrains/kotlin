/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.utils.SmartList
import kotlin.math.min

val EMPTY_LIST = SmartList<Nothing>()

inline fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> {
    if (isEmpty()) return EMPTY_LIST
    if (size == 1) return SmartList(transform(first()))
    return mapTo(ArrayList(size), transform)
}

inline fun <T, R : Any> Collection<T>.mapNotNull(transform: (T) -> R?): List<R> {
    if (isEmpty()) return EMPTY_LIST
    if (size == 1) return transform(first())?.let { SmartList(it) } ?: EMPTY_LIST
    return mapNotNullTo(ArrayList(size), transform)
}

inline fun <T, R> Collection<T>.mapIndexed(transform: (index: Int, T) -> R): List<R> {
    if (isEmpty()) return EMPTY_LIST
    if (size == 1) return SmartList(transform(0, first()))
    return mapIndexedTo(ArrayList<R>(size), transform)
}

inline fun <T, R> Collection<T>.flatMap(transform: (T) -> Iterable<R>): List<R> {
    val result = flatMapTo(ArrayList<R>(), transform)
    return when (result.size) {
        0 -> EMPTY_LIST
        1 -> SmartList(result.first())
        else -> result
    }
}

inline fun <T> Collection<T>.filter(predicate: (T) -> Boolean): List<T> {
    val result = filterTo(ArrayList(), predicate)
    return when (result.size) {
        0 -> EMPTY_LIST
        1 -> SmartList(result.first())
        else -> result
    }
}

inline fun <T> Collection<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    val result = filterNotTo(SmartList(), predicate)
    return when (result.size) {
        0 -> EMPTY_LIST
        1 -> SmartList(result.first())
        else -> result
    }
}

inline fun <reified T> Collection<*>.filterIsInstance(): List<T> {
    val result = filterIsInstanceTo(ArrayList<T>())
    return when (result.size) {
        0 -> EMPTY_LIST
        1 -> SmartList(result.first())
        else -> result
    }
}

operator fun <T> List<T>.plus(elements: List<T>): List<T> {
    if (isEmpty() && elements.isEmpty()) return EMPTY_LIST
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return when (result.size) {
        0 -> EMPTY_LIST
        1 -> SmartList(result.first())
        else -> result
    }
}

infix fun <T, R> Collection<T>.zip(other: Collection<R>): List<Pair<T, R>> {
    if (isEmpty() || other.isEmpty()) return EMPTY_LIST
    if (min(size, other.size) == 1) return SmartList(first() to other.first())
    return zip(other) { t1, t2 -> t1 to t2 }
}

fun <T> Sequence<T>.singleOrNullStrict(): T? {
    val iterator = iterator()
    if (!iterator.hasNext())
        return null
    val single = iterator.next()
    if (iterator.hasNext())
        throw AssertionError("Assertion failed")
    return single
}
