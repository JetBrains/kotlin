/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation

/**
 * Pushes [element] to the top of this list, executes [body] and then pops the last element of the list.
 *
 * If [body] throws an exception, does not pop the last element.
 * This behavior is intentional because you may need to examine the contents of this list when catching the exception somewhere
 * up the stack.
 *
 * [body] is made `crossinline` to disallow non-local control flow, which could lead to the list being in an inconsistent state.
 */
inline fun <E, R> MutableList<E>.temporarilyPushing(element: E, crossinline body: (E) -> R): R {
    this.add(element)
    // Not wrapped in a try/finally because we may need to examine the contents of the list
    // if we catch an exception somewhere up the stack.
    val result = body(element)
    this.removeAt(size - 1)
    return result
}
