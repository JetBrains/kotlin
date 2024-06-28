/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LimitedLinkedListTest {
    @Test
    @DisplayName("adding elements within limit")
    fun withinLimit() {
        val limitedList = LimitedLinkedList<Int>(5)
        assertTrue(limitedList.add(1))
        assertTrue(limitedList.add(2))
        assertTrue(limitedList.add(3))
        assertEquals(listOf(1, 2, 3), limitedList)
    }

    @Test
    @DisplayName("adding elements beyond limit")
    fun beyondLimit() {
        val limitedList = LimitedLinkedList<Int>(3)
        assertTrue(limitedList.add(1))
        assertTrue(limitedList.add(2))
        assertTrue(limitedList.add(3))
        assertTrue(limitedList.add(4))
        assertEquals(listOf(2, 3, 4), limitedList)
    }

    @Test
    @DisplayName("incorrect limit values lead to exception")
    fun incorrectLimitValues() {
        assertThrows(IllegalArgumentException::class.java) {
            LimitedLinkedList<String>(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LimitedLinkedList<String>(-50)
        }
    }
}