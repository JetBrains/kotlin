/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceAndTo(destination: C, predicate: (R) -> Boolean): C {
    for (element in this) {
        if (element is R && predicate(element)) {
            destination.add(element)
        }
    }
    return destination
}

inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapTo(destination: C, transform: (T) -> R): C {
    for (element in this) {
        if (element is T) {
            destination.add(transform(element))
        }
    }
    return destination
}

inline fun <reified T, reified R> Collection<*>.filterIsInstanceMapNotNull(transform: (T) -> R?): Collection<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceMapNotNullTo(SmartList(), transform)
}

inline fun <reified R> Collection<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceAndTo(SmartList(), predicate)
}

inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapNotNullTo(
    destination: C,
    transform: (T) -> R?
): C {
    for (element in this) {
        if (element is T) {
            val result = transform(element)
            if (result != null) {
                destination.add(result)
            }
        }
    }
    return destination
}

/**
 * Returns the single element of the collection if it contains at most one element.
 *
 * If the collection is empty, returns `null`.
 *
 * If the collection contains exactly one element, returns that element.
 *
 * If the collection contains more than one element, throws an exception.
 */
fun <T> Collection<T>.atMostOne(): T? {
    return when (size) {
        0 -> null
        1 -> this.iterator().next()
        else -> throw IllegalArgumentException("Collection has more than one element.")
    }
}

/**
 * Returns at most one element from the iterable that satisfies the given predicate.
 *
 * If there are no elements that satisfy [predicate], returns `null`.
 *
 * If there is exactly one element that satisfies [predicate], returns that element.
 *
 * If there are more such elements, throws an exception.
 */
inline fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean): T? = this.filter(predicate).atMostOne()

fun <K, V> MutableMap<K, MutableList<V>>.putToMultiMap(key: K, value: V) {
    val list = getOrPut(key) { mutableListOf() }
    list.add(value)
}
