/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

internal fun <E : Any> List<E>.mapLazy(conversion: suspend SequenceScope<E>.(E) -> Unit): List<E> =
    LazyList(toList(), conversion)

private class LazyList<E : Any>(
    private val original: List<E>,
    private val conversion: suspend SequenceScope<E>.(E) -> Unit
) : List<E> {
    private val converted = MutableList<E?>(original.size) { null }

    override fun get(index: Int): E = converted[index] ?: run {
        var result = original[index]
        for (stage in iterator { conversion(result) }) {
            result = stage
            converted[index] = stage
        }
        result
    }

    override val size: Int get() = original.size
    override fun contains(element: E): Boolean = any { it == element }
    override fun containsAll(elements: Collection<E>): Boolean = elements.all { it in this }
    override fun isEmpty(): Boolean = original.isEmpty()
    override fun indexOf(element: E): Int = indexOfFirst { it == element }
    override fun lastIndexOf(element: E): Int = indexOfLast { it == element }
    override fun iterator(): Iterator<E> = IteratorImpl(0)
    override fun listIterator(): ListIterator<E> = IteratorImpl(0)
    override fun listIterator(index: Int): ListIterator<E> = IteratorImpl(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<E> = (fromIndex until toIndex).map { this[it] }

    private inner class IteratorImpl(private var index: Int) : ListIterator<E> {
        override fun hasNext(): Boolean = index < size
        override fun hasPrevious(): Boolean = index > 0
        override fun next(): E = get(index++)
        override fun nextIndex(): Int = index
        override fun previous(): E = get(--index)
        override fun previousIndex(): Int = index - 1
    }
}
