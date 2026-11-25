/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XcodeVersionFileTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `parse takes the first line that is not blank or a comment`() {
        assertEquals(XcodeRequest.Version("16.4"), XcodeVersionFile.parse("16.4\n"))
        assertEquals(XcodeRequest.Version("16F6"), XcodeVersionFile.parse("# pinned for CI\n\n  16F6  \n17.0\n"))
    }

    @Test
    fun `parse returns null without a version`() {
        assertNull(XcodeVersionFile.parse(""))
        assertNull(XcodeVersionFile.parse("\n  \n# only a comment\n"))
    }

    @Test
    fun `find walks up to the nearest file`() {
        val root = tempDir.resolve("project with spaces")
        val nested = root.resolve("app/src").createDirectories()
        val file = root.resolve(".xcode-version").apply { writeText("16.4") }

        assertEquals(file, XcodeVersionFile.find(nested, stopAt = tempDir))
        assertEquals(XcodeRequest.Version("16.4"), XcodeVersionFile.read(file))
    }

    @Test
    fun `find prefers the closest file`() {
        val root = tempDir.resolve("root")
        val module = root.resolve("module").createDirectories()
        root.resolve(".xcode-version").writeText("16.4")
        val closest = module.resolve(".xcode-version").apply { writeText("26.0") }

        assertEquals(closest, XcodeVersionFile.find(module, stopAt = tempDir))
    }

    @Test
    fun `find stops at stopAt`() {
        tempDir.resolve(".xcode-version").writeText("16.4")
        val start = tempDir.resolve("a/b").createDirectories()

        assertNull(XcodeVersionFile.find(start, stopAt = tempDir.resolve("a")))
    }

    @Test
    fun `find ignores a directory with the file name`() {
        val start = tempDir.resolve("a").createDirectories()
        start.resolve(".xcode-version").createDirectories()

        assertNull(XcodeVersionFile.find(start, stopAt = tempDir))
    }

    @Test
    fun `find without stopAt walks up from the start directory`() {
        val file = tempDir.resolve(".xcode-version").apply { writeText("16.4") }
        val start = tempDir.resolve("a/b").createDirectories()

        assertEquals(file, XcodeVersionFile.find(start))
    }

    @Test
    fun `an invalid spec is rejected with the file name`() {
        val file = tempDir.resolve(".xcode-version").apply { writeText("16.4-beta\n") }

        val parse = assertFailsWith<IllegalArgumentException> { XcodeVersionFile.parse("Xcode 16.4") }
        val read = assertFailsWith<IllegalArgumentException> { XcodeVersionFile.read(file) }

        assertEquals("'Xcode 16.4' is neither an Xcode version such as 16.4 nor a build number such as 16F6", parse.message)
        assertEquals("$file: '16.4-beta' is neither an Xcode version such as 16.4 nor a build number such as 16F6", read.message)
    }
}
