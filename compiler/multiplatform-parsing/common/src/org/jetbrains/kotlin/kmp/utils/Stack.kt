/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.utils

class Stack<T> {
    private val elements = mutableListOf<T>()

    fun push(item: T) {
        elements.add(item)
    }

    fun pop(): T {
        return elements.removeAt(elements.size - 1)
    }

    fun peek(): T {
        return elements.last()
    }

    fun isEmpty(): Boolean = elements.isEmpty()

    val size: Int
        get() = elements.size
}