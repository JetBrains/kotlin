/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.differences

class MapBasedSetWithCustomKey<K, E>(
    protected val lookup: MutableMap<K, E> = mutableMapOf(),
    private val toKey: (E) -> K,
) : AbstractMutableSet<E>() {
    override fun add(element: E): Boolean {
        val key = toKey(element)

        return if (key !in lookup) {
            lookup[key] = element
            true
        } else {
            false
        }
    }

    override fun clear() = lookup.clear()

    override fun contains(element: E) = toKey(element) in lookup

    override fun remove(element: E) = lookup.remove(toKey(element)) != null

    override fun toString() = "HashSetWithCustomKey${lookup.values}"

    override val size get() = lookup.size

    inner class Iterator : MutableIterator<E> {
        private val delegate = lookup.iterator()

        override fun hasNext() = delegate.hasNext()

        override fun next() = delegate.next().value

        override fun remove() = delegate.remove()
    }

    override fun iterator() = Iterator()
}