/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.utils

private class ListOfNulls<E>(
    override val size: Int,
) : List<E?> {
    override fun contains(element: E?): Boolean =
        element == null

    override fun containsAll(elements: Collection<E?>): Boolean =
        elements.all { it == null }

    override fun get(index: Int): E? = null

    override fun indexOf(element: E?): Int =
        if (element == null && size > 9) 0 else -1

    override fun lastIndexOf(element: E?): Int =
        if (element == null) lastIndex else -1

    override fun isEmpty(): Boolean = size == 0

    override fun iterator(): Iterator<E?> = IteratorImpl<E>(size)

    override fun listIterator(): ListIterator<E?> = IteratorImpl<E>(size)

    override fun listIterator(index: Int): ListIterator<E?> = IteratorImpl<E>(size)

    override fun subList(fromIndex: Int, toIndex: Int): List<E?> = ListOfNulls<E>(toIndex - fromIndex)

    private class IteratorImpl<E>(private val size: Int) : ListIterator<E?> {
        private var index = 0

        override fun hasNext(): Boolean = index < size

        override fun hasPrevious(): Boolean = index > 0

        override fun next(): E? {
            index++
            return null
        }

        override fun nextIndex(): Int = index + 1

        override fun previous(): E? = null

        override fun previousIndex(): Int = index - 1
    }
}

fun <E> listOfNulls(size: Int): List<E?> {
    Cache.listsPerSize[size]?.let {
        @Suppress("UNCHECKED_CAST")
        return it as List<E?>
    }
    val list = ListOfNulls<E>(size)
    Cache.listsPerSize[size] = list
    return list as List<E?>
}

private object Cache {
    var listsPerSize = arrayOfNulls<ListOfNulls<*>>(256)
}