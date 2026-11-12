/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUtilsTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `deletes a tree`() {
        val tree = Files.createDirectories(tempDir.resolve("tree/a b/ü"))
        Files.write(tree.resolve("file"), byteArrayOf(1))

        deleteRecursively(tempDir.resolve("tree"))

        assertFalse(Files.exists(tempDir.resolve("tree")))
    }

    @Test
    fun `a missing path is fine`() {
        deleteRecursively(tempDir.resolve("missing"))
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "creating symlinks needs a privilege on Windows")
    fun `deletes symlinks without following them`() {
        val outside = Files.createDirectories(tempDir.resolve("outside"))
        Files.write(outside.resolve("keep"), byteArrayOf(1))
        val tree = Files.createDirectories(tempDir.resolve("tree"))
        Files.createSymbolicLink(tree.resolve("link"), outside)
        Files.createSymbolicLink(tree.resolve("dangling"), tempDir.resolve("nowhere"))

        deleteRecursively(tree)

        assertFalse(Files.exists(tree))
        assertTrue(Files.exists(outside.resolve("keep")), "what a symlink points to is left alone")
    }

    @Test
    @DisabledOnOs(OS.WINDOWS, disabledReason = "POSIX permissions")
    fun `deletes read-only and unreadable directories`() {
        val tree = Files.createDirectories(tempDir.resolve("tree"))
        for ((name, permissions) in listOf("read-only" to "r-xr-xr-x", "unreadable" to "---------")) {
            val directory = Files.createDirectories(tree.resolve(name))
            Files.write(directory.resolve("file"), byteArrayOf(1))
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString(permissions))
        }

        deleteRecursively(tree)

        assertFalse(Files.exists(tree))
    }
}
