/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.util

class ChainedIterator<T>(delegates: Collection<Iterator<T>>) : Iterator<T> {
    private var metaIterator = delegates.iterator()
    private var currentIterator: Iterator<T>? = null

    private fun promote() {
        if (currentIterator?.hasNext() == true) return
        while (metaIterator.hasNext()) {
            currentIterator = metaIterator.next()
            if (currentIterator!!.hasNext()) return
        }
    }

    override fun hasNext(): Boolean {
        promote()
        return currentIterator?.hasNext() == true
    }

    override fun next(): T {
        promote()
        return currentIterator?.next() ?: throw NoSuchElementException()
    }
}
