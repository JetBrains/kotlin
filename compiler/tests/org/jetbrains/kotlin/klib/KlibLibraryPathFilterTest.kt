/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

class KlibLibraryPathFilterTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun testCanonicalFilterAcceptsLibraryLoadedThroughSymlink() {
        val klib = createKlib(tempDir.resolve("library"))
        val library = loadKlib(createSymlink(tempDir.resolve("library-link"), klib))
        assertNotEquals(library.path, library.canonicalPath)

        val filter = LibraryPathFilter.LibraryList(setOf(library.canonicalPath))
        assertFalse(filter.accepts(library.path))
        assertTrue(filter.accepts(library.canonicalPath))
    }

    @Test
    fun testCanonicalDirectoryFilterAcceptsLibraryLoadedThroughSymlinkedDirectory() {
        val realDirectory = tempDir.resolve("real").createDirectories()
        createKlib(realDirectory.resolve("library"))
        val linkedDirectory = createSymlink(tempDir.resolve("linked"), realDirectory)
        val library = loadKlib(linkedDirectory.resolve("library"))
        assertNotEquals(library.path, library.canonicalPath)

        val filter = LibraryPathFilter.LibraryList(setOf(realDirectory))
        assertFalse(filter.accepts(library.path))
        assertTrue(filter.accepts(realDirectory))
    }

    @Test
    fun testCanonicalFilterRejectsDifferentLibraryLoadedThroughSymlink() {
        val firstKlib = createKlib(tempDir.resolve("first"))
        val secondKlib = createKlib(tempDir.resolve("second"))
        val firstLibrary = loadKlib(createSymlink(tempDir.resolve("first-link"), firstKlib))

        val filter = LibraryPathFilter.LibraryList(setOf(secondKlib.toRealPath()))
        assertFalse(filter.accepts(firstLibrary.path))
    }

    @Test
    fun testLexicalFiltering() {
        val libraryDirectory = tempDir.resolve("libraries")
        val filter = LibraryPathFilter.LibraryList(setOf(libraryDirectory))

        assertAll(
            { assertTrue(filter.accepts(libraryDirectory)) },
            { assertTrue(filter.accepts(libraryDirectory.resolve("library.klib"))) },
            { assertFalse(filter.accepts(tempDir)) },
            { assertFalse(filter.accepts(tempDir.resolve("other-libraries"))) },
            { assertFalse(filter.accepts(null)) },
        )
    }

    @Test
    fun testNonExistingPathsDoNotMatchThroughRealPathFallback() {
        val filter = LibraryPathFilter.LibraryList(setOf(tempDir.resolve("missing-library")))
        assertFalse(filter.accepts(tempDir.resolve("other-missing-library")))
    }

    private fun createKlib(path: Path): Path {
        KlibWriter {
            manifest {
                moduleName(path.fileName.pathString)
                versions(KotlinLibraryVersioning(null, null, null))
                platformAndTargets(BuiltInsPlatform.COMMON)
            }
        }.writeTo(path.pathString)
        return path
    }

    private fun createSymlink(link: Path, target: Path): Path {
        assumeTrue(runCatching { Files.createSymbolicLink(link, target) }.isSuccess)
        return link
    }

    private fun loadKlib(path: Path) = KlibLoader { libraryPaths(path) }.load().let { result ->
        assertFalse(result.hasProblems)
        assertEquals(1, result.librariesStdlibFirst.size)
        result.librariesStdlibFirst.single()
    }
}
