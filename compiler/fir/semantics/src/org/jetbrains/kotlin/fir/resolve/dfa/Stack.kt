/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

abstract class Stack<T> {
    abstract val size: Int
    abstract fun top(): T
    abstract fun pop(): T
    abstract fun push(value: T)
    abstract fun reset()

    /**
     * returns all elements of the stack in order of retrieval
     */
    abstract fun all(): List<T>

    abstract fun <R> makeSnapshot(transform: (T) -> R): Stack<R>
}

fun <T> stackOf(vararg values: T): Stack<T> = StackImpl(*values)
val Stack<*>.isEmpty: Boolean get() = size == 0
val Stack<*>.isNotEmpty: Boolean get() = size != 0
fun <T> Stack<T>.topOrNull(): T? = if (size == 0) null else top()

private class StackImpl<T>(values: List<T>) : Stack<T>() {
    constructor(vararg values: T) : this(values.asList())

    private val values = ArrayList(values)

    override fun top(): T = values[values.size - 1]
    override fun pop(): T = values.removeAt(values.size - 1)

    override fun push(value: T) {
        values.add(value)
    }

    override val size: Int get() = values.size
    override fun reset() {
        values.clear()
    }

    override fun all(): List<T> = values.asReversed()

    override fun <R> makeSnapshot(transform: (T) -> R): Stack<R> {
        val newValues = values.map(transform)
        return StackImpl(newValues)
    }
}
