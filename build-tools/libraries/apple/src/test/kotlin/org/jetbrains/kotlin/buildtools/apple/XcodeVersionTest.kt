/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XcodeVersionTest {

    @Test
    fun `parse fills missing components with zero`() {
        assertEquals(XcodeVersion(16, 0, 0), XcodeVersion.parse("16"))
        assertEquals(XcodeVersion(16, 4, 0), XcodeVersion.parse("16.4"))
        assertEquals(XcodeVersion(16, 4, 1), XcodeVersion.parse(" 16.4.1\n"))
    }

    @Test
    fun `parse rejects anything that is not a version`() {
        listOf("", "Xcode 16.4", "16.4.1.2", "16.", "16.4 beta", "-1", "99999999999").forEach {
            assertNull(XcodeVersion.parse(it), "'$it' should not parse")
        }
    }

    @Test
    fun `toString omits a zero patch`() {
        assertEquals("16.0", XcodeVersion(16).toString())
        assertEquals("16.4", XcodeVersion(16, 4).toString())
        assertEquals("16.4.1", XcodeVersion(16, 4, 1).toString())
    }

    @Test
    fun `versions compare component by component`() {
        assertTrue(XcodeVersion(16, 4, 1) > XcodeVersion(16, 4))
        assertTrue(XcodeVersion(16, 10) > XcodeVersion(16, 9, 9))
        assertTrue(XcodeVersion(26) > XcodeVersion(16, 4))
        assertEquals(0, XcodeVersion(16).compareTo(XcodeVersion(16, 0, 0)))
    }

    @Test
    fun `negative components are rejected`() {
        assertFailsWith<IllegalArgumentException> { XcodeVersion(16, -1) }
    }
}
