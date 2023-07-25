/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.collections

/**
 * Maps all elements of this non-empty collection with the given [transform] function to a new mutable set, or returns [emptySet] if this
 * collection is empty.
 *
 * [mapToSet] should be preferred over `collection.mapTo(mutableSetOf()) { ... }` when `collection` may be empty and the resulting set may
 * be cached, because [mapToSet] saves memory by avoiding the creation of an empty mutable set.
 */
public inline fun <T, R> Collection<T>.mapToSet(transform: (T) -> R): Set<R> =
    if (isNotEmpty()) mapTo(mutableSetOf(), transform) else emptySet()
