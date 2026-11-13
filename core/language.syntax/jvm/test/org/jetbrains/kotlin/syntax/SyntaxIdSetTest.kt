/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SyntaxIdSetTest {
    @Test
    fun testEmpty() {
        val set = syntaxIdSetOf()
        assertSame(emptySyntaxIdSet, set)
        assertTrue(set.isEmpty())
        assertEquals(0, set.size)
        assertFalse(NO_ID in set)
        assertFalse(-1 in set)
        assertFalse(set.contains(null as SyntaxId?))
    }

    @Test
    fun testContainsMatchesHashSet() {
        // Cover word boundaries and IDs far from each other
        val ids = listOf(1, 2, 63, 64, 65, 127, 128, 200, 1000)
        val set = ids.asSyntaxIdSet()
        val expected = ids.toSet()
        for (id in -10..1100) {
            assertEquals(id in expected, id in set, "Mismatch for $id")
        }
        assertEquals(expected, set)
        assertTrue(set == expected, "`equals` must be symmetric")
        assertEquals(expected.hashCode(), set.hashCode())
        assertEquals(set, ids.reversed().asSyntaxIdSet())
        assertEquals(expected.toString(), set.toString())
        assertFalse(set == ids.map { it.toString() }.toSet())
        assertFalse(set == expected - 1)
    }

    @Test
    fun testMinIdNotAtWordStart() {
        val set = syntaxIdSetOf(130, 131, 250)
        assertTrue(130 in set)
        assertTrue(250 in set)
        assertFalse(2 in set) // Same bit index as 130 inside a word
        assertFalse(66 in set)
        assertFalse(129 in set)
        assertFalse(251 in set)
    }

    @Test
    fun testDuplicatesAndNullable() {
        val set = syntaxIdSetOf(5, 5, 7)
        assertEquals(2, set.size)
        assertEquals(listOf(5, 7), set.toList())
        assertTrue(set.contains(5 as SyntaxId?))
        assertFalse(set.contains(null as SyntaxId?))
    }

    @Test
    fun testPlusAndMinus() {
        val set = syntaxIdSetOf(1, 2)
        assertSame(set, set + 1)
        assertSame(set, set + listOf(1, 2))
        assertSame(set, set - 3)
        assertSame(set, set - listOf(3, 4))
        assertEquals(setOf(1, 2, 3), set + 3)
        assertEquals(setOf(1, 2, 3, 100), set + listOf(3, 100))
        assertEquals(setOf(2), set - 1)
        assertEquals(setOf(2), set - listOf(1, 3))
        assertTrue((set - listOf(1, 2)).isEmpty())
    }

    @Test
    fun testAllRealIds() {
        // Not hardcoded values: covers the whole current range of IDs, including the max one, and stays valid when IDs shift
        val idsByName = collectSyntaxIdProperties().associate { it.name to it.value }
        val set = idsByName.values.asSyntaxIdSet()
        assertEquals(idsByName.size, set.size)
        for ([name, id] in idsByName) {
            assertTrue(id in set, "$name ($id) is missing")
        }

        val maxId = idsByName.values.max()
        for (id in -1..maxId + Long.SIZE_BITS) {
            assertEquals(id in idsByName.values, id in set, "Mismatch for $id")
        }
        assertTrue((set - idsByName.values).isEmpty())
    }

    @Test
    fun testNoIdAndNegativeIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { syntaxIdSetOf(NO_ID) }
        assertFailsWith<IllegalArgumentException> { syntaxIdSetOf(1, NO_ID) }
        assertFailsWith<IllegalArgumentException> { syntaxIdSetOf(-1) }
        assertFailsWith<IllegalArgumentException> { syntaxIdSetOf(1) + NO_ID }
        assertFailsWith<IllegalArgumentException> { listOf(NO_ID).asSyntaxIdSet() }
    }
}
