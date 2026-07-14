/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.backend.common.dependencies.util.TraversalScope
import org.jetbrains.kotlin.backend.common.dependencies.util.traversal
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class TraversalIteratorTest {

    @Test
    fun testTraversalIteratorPreOrderDfs() {
        val neighbours: (Int) -> Sequence<Int> = {
            when (it) {
                1 -> sequenceOf(2, 3)
                2 -> sequenceOf(4, 5)
                3 -> sequenceOf(6, 7)
                4 -> sequenceOf(8, 9, 10)
                else -> emptySequence()
            }
        }

        suspend fun TraversalScope<Int, Int>.traverseNext(element: Int, neighbours: (Int) -> Sequence<Int>) {
            emit(element)
            neighbours(element).forEach { traverseFor(it) }
        }

        val dfs = traversal(1) { element ->
            traverseNext(element, neighbours)
        }
        assertEquals(listOf(1, 2, 4, 8, 9, 10, 5, 3, 6, 7), dfs.toList())
    }

    @Test
    fun testTraversalIteratorEmpty() {
        val empty = traversal<Int, Int>(0) { }
        assertEquals(emptyList(), empty.toList())
    }

    @Test
    fun testTraversalIteratorRecursiveException() {
        val neighbours: (Int) -> Sequence<Int> = {
            when (it) {
                1 -> sequenceOf(2, 3)
                2 -> sequenceOf(4, 5)
                3 -> sequence { throw RuntimeException("Recursive iterator exception") }
                else -> emptySequence()
            }
        }
        val empty = traversal(1) { element ->
            emit(element)
            neighbours(element).forEach { traverseFor(it) }
        }
        assertThrows<RuntimeException> { empty.toList() }
    }
}
