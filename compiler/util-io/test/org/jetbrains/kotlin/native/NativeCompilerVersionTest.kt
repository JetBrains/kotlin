/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.CompilerVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.parseCompilerVersion
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.*

class NativeCompilerVersionTest {

    @Test
    fun versionParseTest() {
        "1.5.30-dev-1".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.DEV, meta)
            assertEquals(1, build)
        }
        "1.5.30-rc-12".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.RC, meta)
            assertEquals(12, build)
        }
        // Final release
        "1.5.30".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.RELEASE, meta)
            assertEquals(-1, build)
        }
        // Final release build.number
        "1.5.30-release-34".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.RELEASE, meta)
            assertEquals(34, build)
        }
        // Release builds
        "1.5.30-1234".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.RELEASE, meta)
            assertEquals(1234, build)
        }
        "1.6.0-dev-1".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(6, minor)
            assertEquals(0, maintenance)
            assertEquals(MetaVersion.DEV, meta)
            assertEquals(1, build)
        }
        "1.6.10-M1-3".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(6, minor)
            assertEquals(10, maintenance)
            assertEquals(MetaVersion.M1, meta)
            assertEquals(3, build)
        }
        "1.6.20-M2".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(6, minor)
            assertEquals(20, maintenance)
            assertEquals(MetaVersion.M2, meta)
            assertEquals(-1, build)
        }
        "1.6.10-RC".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(6, minor)
            assertEquals(10, maintenance)
            assertEquals(MetaVersion.RC, meta)
            assertEquals(-1, build)
        }
    }

    @Test
    fun incorrectVersionString() {
        assertFailsWith<IllegalArgumentException> { CompilerVersion.fromString("1.5.30-M1-dev-123") }
        assertFailsWith<IllegalArgumentException> { CompilerVersion.fromString("1.5.30-M1-release-123") }
        assertFailsWith<IllegalArgumentException> { CompilerVersion.fromString("1.5.30.40-release-123") }
        assertFailsWith<IllegalStateException> { CompilerVersion.fromString("1.6.0-M3-123") }
    }

    @Test
    fun versionToString() {
        assertEquals("1.5.30-dev-140", CompilerVersionImpl(MetaVersion.DEV, 1, 5, 30, -1, 140).toString())
        assertEquals("1.5.30-RC-140", CompilerVersionImpl(MetaVersion.RC, 1, 5, 30, -1, 140).toString())
        assertEquals("1.5.30-RC", CompilerVersionImpl(MetaVersion.RC, 1, 5, 30, -1, -1).toString())
        assertEquals("1.6.10-14", CompilerVersionImpl(MetaVersion.RELEASE, 1, 6, 10, -1, 14).toString())
        assertEquals("1.6.10-14", CompilerVersionImpl(MetaVersion.RELEASE, 1, 6, 10, -1, 14).toString())
        assertEquals("1.6.0", CompilerVersionImpl(MetaVersion.RELEASE, 1, 6, 0, -1, -1).toString())
    }

    @Test
    fun publicVersion() {
        "1.5.30-dev-google-pr-123".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.DEV_GOOGLE, meta)
            assertEquals(123, build)
        }
        assertEquals("1.5.30-dev-google-pr-140", CompilerVersionImpl(MetaVersion.DEV_GOOGLE, 1, 5, 30, -1, 140).toString())

        "1.5.30-pub-123".parseCompilerVersion().apply {
            assertEquals(1, major)
            assertEquals(5, minor)
            assertEquals(30, maintenance)
            assertEquals(MetaVersion.PUB, meta)
            assertEquals(123, build)
        }
        assertEquals("1.5.30-PUB-140", CompilerVersionImpl(MetaVersion.PUB, 1, 5, 30, -1, 140).toString())
    }
}