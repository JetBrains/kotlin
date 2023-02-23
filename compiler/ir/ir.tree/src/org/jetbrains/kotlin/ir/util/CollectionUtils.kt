/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

val EMPTY_LIST = ArrayList<Nothing>(0)

inline fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> {
    if (isEmpty()) return EMPTY_LIST
    return mapTo(ArrayList(size), transform)
}

inline fun <T, R : Any> Collection<T>.mapNotNull(transform: (T) -> R?): List<R> {
    if (isEmpty()) return EMPTY_LIST
    return mapNotNullTo(ArrayList(size), transform)
}

inline fun <T, R> Collection<T>.mapIndexed(transform: (index: Int, T) -> R): List<R> {
    if (isEmpty()) return EMPTY_LIST
    return mapIndexedTo(ArrayList<R>(size), transform)
}

inline fun <T, R> Collection<T>.flatMap(transform: (T) -> Iterable<R>): List<R> {
    if (isEmpty()) return EMPTY_LIST
    return flatMapTo(ArrayList<R>(), transform)
}

inline fun <T> Collection<T>.filter(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return EMPTY_LIST
    return filterTo(ArrayList(), predicate)
}

inline fun <T> Collection<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return EMPTY_LIST
    return filterNotTo(ArrayList(), predicate)
}

inline fun <reified T> Collection<*>.filterIsInstance(): List<T> {
    if (isEmpty()) return EMPTY_LIST
    return filterIsInstanceTo(ArrayList<T>())
}

operator fun <T> List<T>.plus(elements: List<T>): List<T> {
    if (isEmpty() && elements.isEmpty()) return EMPTY_LIST
    val result = ArrayList<T>(this.size + elements.size)
    result.addAll(this)
    result.addAll(elements)
    return result
}

infix fun <T, R> Collection<T>.zip(other: Collection<R>): List<Pair<T, R>> {
    if (isEmpty() || other.isEmpty()) return EMPTY_LIST
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
