/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * Works almost as regular flatMap, but returns a set and returns null if any lambda call returned null
 */
inline fun <T, R> Iterable<T>.flatMapToNullableSet(transform: (T) -> Iterable<R>?): Set<R>? =
    flatMapTo(mutableSetOf()) { transform(it) ?: return null }.ifEmpty { emptySet() }

/**
 * Maps all elements of this non-empty collection with the given [transform] function to a new mutable set, or returns [emptySet] if this
 * collection is empty.
 *
 * [mapToSetOrEmpty] should be preferred over `collection.mapTo(mutableSetOf()) { ... }` when `collection` may be empty and the resulting
 * set may be cached, because [mapToSetOrEmpty] saves memory by avoiding the creation of an empty mutable set.
 */
inline fun <T, R> Collection<T>.mapToSetOrEmpty(transform: (T) -> R): Set<R> =
    if (isNotEmpty()) mapTo(mutableSetOf(), transform) else emptySet()

inline fun <T> Collection<T>.filterToSetOrEmpty(predicate: (T) -> Boolean): Set<T> =
    filterTo(mutableSetOf(), predicate).ifEmpty { emptySet() }
