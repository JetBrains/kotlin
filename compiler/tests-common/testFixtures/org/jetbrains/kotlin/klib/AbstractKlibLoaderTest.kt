/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.collections.forEach
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.relativeTo
import org.jetbrains.kotlin.konan.file.File as KFile

abstract class AbstractKlibLoaderTest {
    protected lateinit var tmpDir: File
        private set

    /** Path to platform-specific stdlib. */
    protected abstract val stdlib: String

    private var generatedLibsCounter = 0

    // These are the paths that are invalid in macOS, Linux and Windows:
    private val invalidPaths: List<String>
        get() = listOf(
            "foo\u0000bar",
            ""
        )

    private val nonExistingPaths: List<String>
        get() = listOf(
            tmpDir.resolve("non-existing-library1").path,
            "non-existing-library2",
            "../non-existing-library3",
            "./non-existing-library4",
        )

    @Suppress("LocalVariableName")
    private val corruptedLibraryPaths: List<String> by lazy {
        buildList {
            // Just an empty directory.
            this += tmpDir.resolve("corrupted-library1").apply { mkdirs() }

            // Just an empty file.
            this += tmpDir.resolve("corrupted-library2").apply { createNewFile() }

            // Copy of a real KLIB without the "default" component. As a directory.
            val noDefaultComponentDir = tmpDir.resolve("corrupted-library3")
            File(stdlib).copyRecursively(noDefaultComponentDir)
            with(noDefaultComponentDir.resolve("default")) { renameTo(resolveSibling("non-default")) }
            this += noDefaultComponentDir

            // Copy of a real KLIB without the "default" component. As a file.
            val noDefaultComponentFile = tmpDir.resolve("corrupted-library4")
            KFile(noDefaultComponentDir.path).zipDirAs(KFile(noDefaultComponentFile.path))
            this += noDefaultComponentFile

            // Copy of a real KLIB without a manifest. As a directory.
            val noManifestDir = tmpDir.resolve("corrupted-library5")
            File(stdlib).copyRecursively(noManifestDir)
            noManifestDir.resolve("default/manifest").delete()
            this += noManifestDir

            // Copy of a real KLIB without a manifest. As a file.
            val noManifestFile = tmpDir.resolve("corrupted-library6")
            KFile(noManifestDir.path).zipDirAs(KFile(noManifestFile.path))
            this += noManifestFile
        }.flatMap { libraryFile ->
            val libraryPath = libraryFile.path

            // Just make a copy of the original file/directory but with an extension.
            val libraryFile_klib = File("$libraryPath.klib")
            libraryFile.copyRecursively(libraryFile_klib)

            val libraryFile_txt = File("$libraryPath.txt")
            libraryFile.copyRecursively(libraryFile_txt)

            listOf(libraryFile, libraryFile_klib, libraryFile_txt)
        }.map { it.path }
    }

    @BeforeEach
    fun setup(info: TestInfo) {
        tmpDir = KtTestUtil.tmpDirForTest(info.testClass.get().simpleName, info.testMethod.get().name)
    }

    @Test
    fun testNoPathsToResolve() {
        KlibLoader {}.load()
            .assertNoLoadedLibraries()
            .assertNoProblematicLibraries()
    }

    @Test
    fun testInvalidAndNonExistingPaths() {
        KlibLoader {
            libraryPaths(invalidPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = invalidPaths)

        KlibLoader {
            libraryPaths(invalidPaths + invalidPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = invalidPaths)

        KlibLoader {
            libraryPaths(nonExistingPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = nonExistingPaths)

        KlibLoader {
            libraryPaths(nonExistingPaths + nonExistingPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = nonExistingPaths)

        KlibLoader {
            libraryPaths(invalidPaths + nonExistingPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = invalidPaths + nonExistingPaths)

        KlibLoader {
            libraryPaths(nonExistingPaths + invalidPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(notFoundPaths = nonExistingPaths + invalidPaths)
    }

    @Test
    fun testCorruptedLibraries() {
        KlibLoader {
            libraryPaths(corruptedLibraryPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(invalidFormatPaths = corruptedLibraryPaths)

        KlibLoader {
            libraryPaths(corruptedLibraryPaths + corruptedLibraryPaths)
        }.load()
            .assertNoLoadedLibraries()
            .assertProblematicLibraries(invalidFormatPaths = corruptedLibraryPaths)
    }

    @Test
    fun testValidLibraries() {
        // no extension, but both file and directory KLIBs should be valid:
        val a = generateNewKlib(asFile = false, fileExtension = "")
        val b = generateNewKlib(asFile = true, fileExtension = "")

        // "klib" extension, but both file and directory KLIBs should be valid:
        val c = generateNewKlib(asFile = false, fileExtension = "klib")
        val d = generateNewKlib(asFile = true, fileExtension = "klib")

        // irrelevant extension, but still KLIBs should still be valid:
        val e = generateNewKlib(asFile = false, fileExtension = "txt")
        val f = generateNewKlib(asFile = true, fileExtension = "txt")

        KlibLoader {
            libraryPaths(stdlib, a, b, c, d, e, f)
        }.load()
            .assertLoadedLibraries(stdlib, a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, stdlib, b, c, d, e, f)
        }.load()
            .assertLoadedLibraries(stdlib, a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, b, c, d, stdlib, e, f)
        }.load()
            .assertLoadedLibraries(stdlib, a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, b, c, d, e, f, stdlib)
        }.load()
            .assertLoadedLibraries(stdlib, a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, b, c, d, e, f)
        }.load()
            .assertLoadedLibraries(a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, b, a, c, a, b, d, e, b, f, b, b, f)
        }.load()
            .assertLoadedLibraries(a, b, c, d, e, f)

        KlibLoader {
            libraryPaths(a, b, a, c, a, b, d, stdlib, e, b, f, b, b, f)
        }.load()
            .assertLoadedLibraries(stdlib, a, b, c, d, e, f)
    }

    @Test
    fun testMixedLibraries() {
        val a = generateNewKlib(asFile = false, fileExtension = "")
        val b = generateNewKlib(asFile = true, fileExtension = "klib")

        KlibLoader {
            libraryPaths(a)
            libraryPaths(corruptedLibraryPaths)
            libraryPaths(stdlib)
            libraryPaths(invalidPaths)
            libraryPaths(b)
        }.load()
            .assertLoadedLibraries(stdlib, a, b)
            .assertProblematicLibraries(
                notFoundPaths = invalidPaths,
                invalidFormatPaths = corruptedLibraryPaths
            )
    }

    @Test
    fun testRelativePaths1() {
        val foo: Path = Paths.get(generateNewKlib(asFile = false, fileExtension = ""))
        val bar: Path = Paths.get(nonExistingPaths.first())
        val baz: Path = Paths.get(generateNewKlib(asFile = true, fileExtension = "klib"))
        val qux: Path = Paths.get(corruptedLibraryPaths.first())

        val absolutePaths: List<Path> = listOf(foo, bar, baz, qux)
        absolutePaths.forEach {
            if (it.exists()) {
                assertEquals(it.toRealPath(), it)
            }
            assertTrue(it.startsWith(tmpDir.path))
        }

        val cwd: Path = Paths.get("").toRealPath()

        val transformations: List<(Path) -> Path> = listOf(
            { it },                 // no changes, absolute paths
            { it.relativeTo(cwd) }, // relative paths to `tmpDir`
            { Paths.get("./").resolve(it.relativeTo(cwd)) },
            { runIf(it.isDirectory()) { it.relativeTo(cwd).resolve("..").resolve(it.last()) } ?: it },
        )

        for (transformation in transformations) {
            val transformedPaths = absolutePaths.map { transformation(it).toString() }
            KlibLoader {
                libraryPaths(transformedPaths)
            }.load()
                .assertLoadedLibraries(foo, baz)                      // check against the original (absolute) paths
                .assertProblematicLibraries(
                    notFoundPaths = listOf(transformation(bar)),      // check against transformed paths
                    invalidFormatPaths = listOf(transformation(qux)), // check against transformed paths
                )
        }
    }

    /**
     * This test is needed to ensure that [KlibLoader] does not mix up libraries without extension and with "klib" extension,
     * and always treats them as distinct libraries, even if they have repeating "unique names".
     */
    @Suppress("LocalVariableName")
    @Test
    fun testNoFileExtensionHeuristics() {
        val libsDir = tmpDir.resolve("libs-with-distinct-names").apply { mkdirs() }

        val foo = libsDir.resolve("foo").path
        val foo_klib = libsDir.resolve("foo.klib").path
        val bar = libsDir.resolve("bar").path
        val bar_klib = libsDir.resolve("bar.klib").path
        val baz = libsDir.resolve("baz").path
        val baz_klib = libsDir.resolve("baz.klib").path
        val qux = libsDir.resolve("qux").path
        val qux_klib = libsDir.resolve("qux.klib").path

        with(File(generateNewKlib(asFile = false, fileExtension = ""))) {
            copyRecursively(File(foo))
            copyRecursively(File(foo_klib))
            copyRecursively(File(bar))
            copyRecursively(File(baz_klib))
        }

        with(File(generateNewKlib(asFile = true, fileExtension = "klib"))) {
            copyRecursively(File(bar_klib))
            copyRecursively(File(baz))
            copyRecursively(File(qux))
            copyRecursively(File(qux_klib))
        }

        assertEquals(
            listOf("bar", "bar.klib", "baz", "baz.klib", "foo", "foo.klib", "qux", "qux.klib"),
            libsDir.list().orEmpty().sorted()
        )

        KlibLoader {
            libraryPaths(foo, foo_klib)
            libraryPaths(bar, bar_klib)
            libraryPaths(baz, baz_klib)
            libraryPaths(qux, qux_klib)
        }.load()
            .assertLoadedLibraries(foo, foo_klib, bar, bar_klib, baz, baz_klib, qux, qux_klib)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(foo_klib, foo)
            libraryPaths(bar_klib, bar)
            libraryPaths(baz_klib, baz)
            libraryPaths(qux_klib, qux)
        }.load()
            .assertLoadedLibraries(foo_klib, foo, bar_klib, bar, baz_klib, baz, qux_klib, qux)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(bar, bar_klib)
            libraryPaths(baz, baz_klib)
            libraryPaths(qux, qux_klib)
            libraryPaths(foo, foo_klib)
        }.load()
            .assertLoadedLibraries(bar, bar_klib, baz, baz_klib, qux, qux_klib, foo, foo_klib)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(bar_klib, bar)
            libraryPaths(baz_klib, baz)
            libraryPaths(qux_klib, qux)
            libraryPaths(foo_klib, foo)
        }.load()
            .assertLoadedLibraries(bar_klib, bar, baz_klib, baz, qux_klib, qux, foo_klib, foo)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(baz, baz_klib)
            libraryPaths(qux, qux_klib)
            libraryPaths(foo, foo_klib)
            libraryPaths(bar, bar_klib)
        }.load()
            .assertLoadedLibraries(baz, baz_klib, qux, qux_klib, foo, foo_klib, bar, bar_klib)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(baz_klib, baz)
            libraryPaths(qux_klib, qux)
            libraryPaths(foo_klib, foo)
            libraryPaths(bar_klib, bar)
        }.load()
            .assertLoadedLibraries(baz_klib, baz, qux_klib, qux, foo_klib, foo, bar_klib, bar)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(qux, qux_klib)
            libraryPaths(foo, foo_klib)
            libraryPaths(bar, bar_klib)
            libraryPaths(baz, baz_klib)
        }.load()
            .assertLoadedLibraries(qux, qux_klib, foo, foo_klib, bar, bar_klib, baz, baz_klib)
            .assertNoProblematicLibraries()

        KlibLoader {
            libraryPaths(qux_klib, qux)
            libraryPaths(foo_klib, foo)
            libraryPaths(bar_klib, bar)
            libraryPaths(baz_klib, baz)
        }.load()
            .assertLoadedLibraries(qux_klib, qux, foo_klib, foo, bar_klib, bar, baz_klib, baz)
            .assertNoProblematicLibraries()
    }

    @Test
    fun testRelativePaths2() {
        val lib: Path = Paths.get(generateNewKlib(asFile = false, fileExtension = ""))

        assertEquals(lib.toRealPath(), lib)
        assertTrue(lib.startsWith(tmpDir.path))

        val cwd: Path = Paths.get("").toRealPath()

        val equivalentPaths: List<String> = listOf(
            lib.relativeTo(cwd),                                   // the path relative for `tmpDir`
            Paths.get("./").resolve(lib.relativeTo(cwd)),          // the path relative for `tmpDir`
            lib,                                                   // the original absolute path
            lib.relativeTo(cwd).resolve("..").resolve(lib.last()), // the path relative for `tmpDir`
        ).map { it.toString() }

        assertEquals(equivalentPaths.size, equivalentPaths.toSet().size)

        KlibLoader {
            libraryPaths(equivalentPaths)
        }.load()
            .assertLoadedLibraries(lib) // check against original (absolute) paths
            .assertNoProblematicLibraries()
    }

    @Test
    fun testMaxPermittedAbiVersion() {
        // This list of ABI versions only starts from the current version.
        // Thus, it contains 4 more versions that are definitely not supported by the current compiler.
        val abiVersionsStartingFromCurrent: List<KotlinAbiVersion> =
            generateSequence(KotlinAbiVersion.CURRENT) { it.next() }.take(5).toList()

        val abiVersionsToLibraryPaths: List<Pair<KotlinAbiVersion, String>> = abiVersionsStartingFromCurrent.map { abiVersion ->
            val library = generateNewKlib(asFile = false, fileExtension = "", abiVersion = abiVersion)
            abiVersion to library
        }

        val libraryPaths: List<String> = abiVersionsToLibraryPaths.map { (_, libraryPath) -> libraryPath }

        // Load without ABI version check.
        KlibLoader {
            libraryPaths(libraryPaths)
        }.load()
            .assertLoadedLibraries(libraryPaths) // All libraries are loaded.
            .assertNoProblematicLibraries()
            .run {
                // Check that the requested ABI versions are indeed written to KLIBs.
                (abiVersionsStartingFromCurrent zip librariesStdlibFirst).forEach { (abiVersion, library) ->
                    assertEquals(abiVersion, library.versions.abiVersion)
                }
            }

        for (i in abiVersionsStartingFromCurrent.indices) {
            KlibLoader {
                libraryPaths(libraryPaths)
                maxPermittedAbiVersion(abiVersionsStartingFromCurrent[i])
            }.load()
                .assertLoadedLibraries(libraryPaths.take(i + 1))
                .assertProblematicLibraries(incompatibleAbiVersionPaths = libraryPaths.drop(i + 1))
        }
    }

    @Test
    fun testMaxPermittedAbiVersionAndNoAbiVersionInManifest() {
        // This list of ABI versions only starts from the current version.
        // Thus, it contains 4 more versions that are definitely not supported by the current compiler.
        val abiVersionsStartingFromCurrent: List<KotlinAbiVersion> =
            generateSequence(KotlinAbiVersion.CURRENT) { it.next() }.take(5).toList()

        val libraryPath = generateNewKlib(asFile = false, fileExtension = "")

        // There is no ability to save no ABI version in manifest at all.
        // Thus, we need to patch the manifest manually.
        val manifestFile = File(libraryPath).resolve("default/manifest")
        Properties().apply {
            manifestFile.inputStream().use { load(it) }
            assertTrue(containsKey(KLIB_PROPERTY_ABI_VERSION))
            remove(KLIB_PROPERTY_ABI_VERSION)
            assertFalse(containsKey(KLIB_PROPERTY_ABI_VERSION))
            manifestFile.outputStream().use { store(it, null) }
        }

        // Load without ABI version check.
        KlibLoader {
            libraryPaths(libraryPath)
        }.load()
            .assertLoadedLibraries(libraryPath) // The library are loaded.
            .assertNoProblematicLibraries()
            .run {
                assertEquals(null, librariesStdlibFirst.single().versions.abiVersion)
            }

        for (i in abiVersionsStartingFromCurrent.indices) {
            KlibLoader {
                libraryPaths(libraryPath)
                maxPermittedAbiVersion(abiVersionsStartingFromCurrent[i])
            }.load()
                .assertNoLoadedLibraries()
                .assertProblematicLibraries(incompatibleAbiVersionPaths = listOf(libraryPath))
        }
    }

    @Test
    fun testPlatformCheckers() {
        val a = generateNewKlib(asFile = false, fileExtension = "")
        val b = generateNewKlib(asFile = false, fileExtension = "")
        val c = generateNewKlib(asFile = false, fileExtension = "")

        assertTrue(ownPlatformCheckers.isNotEmpty())
        assertTrue(alienPlatformCheckers.isNotEmpty())

        (listOf(null) + ownPlatformCheckers).forEach { checker ->
            KlibLoader {
                libraryPaths(a, b, c)
                if (checker != null) platformChecker(checker)
            }.load()
                .assertLoadedLibraries(a, b, c)
                .assertNoProblematicLibraries()
        }

        alienPlatformCheckers.forEach { checker ->
            KlibLoader {
                libraryPaths(a, b, c)
                platformChecker(checker)
            }.load()
                .assertNoLoadedLibraries()
                .assertProblematicLibraries(platformCheckMismatchPaths = listOf(a, b, c))
        }
    }

    protected abstract val ownPlatformCheckers: List<KlibPlatformChecker>
    protected abstract val alienPlatformCheckers: List<KlibPlatformChecker>

    private fun KotlinAbiVersion.next() = KotlinAbiVersion(major, minor + 1, patch)

    private fun KlibLoaderResult.assertNoLoadedLibraries(): KlibLoaderResult {
        assertTrue(librariesStdlibFirst.isEmpty())
        return this
    }

    private fun KlibLoaderResult.assertLoadedLibraries(libraryPaths: List<String>): KlibLoaderResult {
        assertEquals(libraryPaths.size, librariesStdlibFirst.size)

        val stdlib: KotlinLibrary? = librariesStdlibFirst.firstOrNull()?.takeIf { it.isAnyPlatformStdlib }
        val otherLibraries: List<KotlinLibrary> = if (stdlib != null) librariesStdlibFirst.drop(1) else librariesStdlibFirst

        var stdlibExpectedInPaths = stdlib != null

        val otherLibrariesCanonicalPaths = libraryPaths.mapNotNull { libraryPath ->
            val canonicalLibraryPath: String = File(libraryPath).canonicalPath

            if (canonicalLibraryPath == stdlib?.libraryFile?.canonicalPath) {
                assertTrue(stdlibExpectedInPaths)
                stdlibExpectedInPaths = false
                return@mapNotNull null
            }

            canonicalLibraryPath
        }

        assertEquals(otherLibrariesCanonicalPaths, otherLibraries.map { it.libraryFile.canonicalPath })

        return this
    }

    private fun KlibLoaderResult.assertLoadedLibraries(vararg libraryPaths: String): KlibLoaderResult =
        assertLoadedLibraries(libraryPaths.toList())

    private fun KlibLoaderResult.assertLoadedLibraries(vararg libraryPaths: Path): KlibLoaderResult =
        assertLoadedLibraries(libraryPaths.map { it.toString() })

    private fun KlibLoaderResult.assertNoProblematicLibraries(): KlibLoaderResult {
        assertFalse(hasProblems)
        assertTrue(problematicLibraries.isEmpty())
        return this
    }

    private fun KlibLoaderResult.assertProblematicLibraries(
        notFoundPaths: List<String> = emptyList(),
        invalidFormatPaths: List<String> = emptyList(),
        platformCheckMismatchPaths: List<String> = emptyList(),
        incompatibleAbiVersionPaths: List<String> = emptyList(),
    ): KlibLoaderResult {
        assertEquals(
            notFoundPaths.isNotEmpty() ||
                    invalidFormatPaths.isNotEmpty() ||
                    platformCheckMismatchPaths.isNotEmpty() ||
                    incompatibleAbiVersionPaths.isNotEmpty(),
            hasProblems
        )
        assertEquals(
            notFoundPaths.size +
                    invalidFormatPaths.size +
                    platformCheckMismatchPaths.size +
                    incompatibleAbiVersionPaths.size,
            problematicLibraries.size
        )

        assertEquals(notFoundPaths, allByCase<ProblemCase.LibraryNotFound>())
        assertEquals(invalidFormatPaths, allByCase<ProblemCase.InvalidLibraryFormat>())
        assertEquals(platformCheckMismatchPaths, allByCase<ProblemCase.PlatformCheckMismatch>())
        assertEquals(incompatibleAbiVersionPaths, allByCase<ProblemCase.IncompatibleAbiVersion>())

        return this
    }

    @JvmName("assertProblematicLibrariesPaths")
    private fun KlibLoaderResult.assertProblematicLibraries(
        notFoundPaths: List<Path> = emptyList(),
        invalidFormatPaths: List<Path> = emptyList(),
        platformCheckMismatchPaths: List<Path> = emptyList(),
        incompatibleAbiVersionPaths: List<Path> = emptyList(),
    ): KlibLoaderResult = assertProblematicLibraries(
        notFoundPaths = notFoundPaths.map { it.toString() },
        invalidFormatPaths = invalidFormatPaths.map { it.toString() },
        platformCheckMismatchPaths = platformCheckMismatchPaths.map { it.toString() },
        incompatibleAbiVersionPaths = incompatibleAbiVersionPaths.map { it.toString() },
    )

    private inline fun <reified T : ProblemCase> KlibLoaderResult.allByCase(): List<String> =
        problematicLibraries.filter { it.problemCase is T }.map { it.libraryPath }

    private fun generateNewKlib(asFile: Boolean, fileExtension: String, abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT): String {
        val uid = (generatedLibsCounter++).toString().padStart(3, '0')
        val baseName = "klib-as_${if (asFile) "file" else "dir"}-ext_$fileExtension-$uid"

        val sourceFile = tmpDir.resolve("$baseName.kt")
        sourceFile.writeText("private fun f() = Unit")

        val klibLocation = tmpDir.resolve(if (fileExtension.isNotEmpty()) "$baseName.$fileExtension" else baseName)
        assertFalse(klibLocation.exists()) { "KLIB should not exist before compilation: $klibLocation" }

        compileKlib(
            asFile = asFile,
            sourceFile = sourceFile,
            klibLocation = klibLocation,
            abiVersion = abiVersion
        )

        // Sometimes the compiler sets file extension on its own. This needs to be fixed specifically for KLIB loader tests.
        if (asFile && !klibLocation.exists()) {
            val altKlibLocation = klibLocation.resolveSibling(klibLocation.nameWithoutExtension + ".klib")
            if (altKlibLocation.exists()) altKlibLocation.renameTo(klibLocation)
        }

        assertTrue(klibLocation.exists()) { "KLIB should exist after compilation: $klibLocation" }
        assertEquals(fileExtension, klibLocation.extension)

        return klibLocation.path
    }

    protected abstract fun compileKlib(asFile: Boolean, sourceFile: File, klibLocation: File, abiVersion: KotlinAbiVersion)
}
