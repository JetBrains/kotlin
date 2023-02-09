/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

inline fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> {
    if (isEmpty()) return emptyList()
    return mapTo(ArrayList(size), transform)
}

inline fun <T, R : Any> Collection<T>.mapNotNull(transform: (T) -> R?): List<R> {
    if (isEmpty()) return emptyList()
    return mapNotNullTo(ArrayList(size), transform)
}

public inline fun <T> Collection<T>.filter(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return emptyList()
    return filterTo(ArrayList(), predicate)
}

public inline fun <T> Collection<T>.filterNot(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return emptyList()
    return filterNotTo(ArrayList(), predicate)
}