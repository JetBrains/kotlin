/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.readProperties
import org.jetbrains.kotlin.io.writeProperties
import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.library.loader.DefaultKlibLibraryProvider
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.collections.forEach
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

abstract class AbstractKlibLoaderTest {
    protected lateinit var tmpDir: Path
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
            tmpDir.resolve("non-existing-library1").pathString,
            "non-existing-library2",
            "../non-existing-library3",
            "./non-existing-library4",
        )

    @OptIn(ExperimentalPathApi::class)
    @Suppress("LocalVariableName")
    private val corruptedLibraryPaths: List<String> by lazy {
        buildList<Path> {
            // Just an empty directory.
            add(tmpDir.resolve("corrupted-library1").apply { createDirectories() })

            // Just an empty file.
            add(tmpDir.resolve("corrupted-library2").apply { createFile() })

            // Copy of a real KLIB without the "default" component. As a directory.
            val noDefaultComponentDir = tmpDir.resolve("corrupted-library3")
            Path(stdlib).copyToRecursively(noDefaultComponentDir, followLinks = false, overwrite = true)
            with(noDefaultComponentDir.resolve("default")) { moveTo(resolveSibling("non-default")) }
            add(noDefaultComponentDir)

            // Copy of a real KLIB without the "default" component. As a file.
            val noDefaultComponentFile = tmpDir.resolve("corrupted-library4")
            noDefaultComponentDir.zipDirAs(noDefaultComponentFile)
            add(noDefaultComponentFile)

            // Copy of a real KLIB without a manifest. As a directory.
            val noManifestDir = tmpDir.resolve("corrupted-library5")
            Path(stdlib).copyToRecursively(noManifestDir, followLinks = false, overwrite = true)
            noManifestDir.resolve("default/manifest").deleteIfExists()
            add(noManifestDir)

            // Copy of a real KLIB without a manifest. As a file.
            val noManifestFile = tmpDir.resolve("corrupted-library6")
            noManifestDir.zipDirAs(noManifestFile)
            add(noManifestFile)
        }.flatMap { libraryFile ->
            val libraryPath = libraryFile.pathString

            // Just make a copy of the original file/directory but with an extension.
            val libraryFile_klib = Path("$libraryPath.klib")
            libraryFile.copyToRecursively(libraryFile_klib, followLinks = false, overwrite = true)

            val libraryFile_txt = Path("$libraryPath.txt")
            libraryFile.copyToRecursively(libraryFile_txt, followLinks = false, overwrite = true)

            listOf(libraryFile, libraryFile_klib, libraryFile_txt)
        }.map { it.pathString }
    }

    @BeforeEach
    fun setup(info: TestInfo) {
        tmpDir = Files.createTempDirectory(info.testClass.get().simpleName + "-" + info.testMethod.get().name).toRealPath()
    }

    @AfterEach
    @OptIn(ExperimentalPathApi::class)
    fun tearDown() {
        tmpDir.deleteRecursively()
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
            assertTrue(it.startsWith(tmpDir))
        }

        val cwd: Path = Paths.get("").toRealPath()

        val transformations: List<(Path) -> Path> = listOf(
            { it },                 // no changes, absolute paths
            { it.relativeTo(cwd) }, // relative paths to `tmpDir`
            { Paths.get("./").resolve(it.relativeTo(cwd)) },
            {
                if (it.isDirectory()) {
                    it.relativeTo(cwd).resolve("..").resolve(it.last())
                } else it
            },
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
    @OptIn(ExperimentalPathApi::class)
    @Suppress("LocalVariableName")
    @Test
    fun testNoFileExtensionHeuristics() {
        val libsDir = tmpDir.resolve("libs-with-distinct-names").apply { createDirectories() }

        val foo = libsDir.resolve("foo").pathString
        val foo_klib = libsDir.resolve("foo.klib").pathString
        val bar = libsDir.resolve("bar").pathString
        val bar_klib = libsDir.resolve("bar.klib").pathString
        val baz = libsDir.resolve("baz").pathString
        val baz_klib = libsDir.resolve("baz.klib").pathString
        val qux = libsDir.resolve("qux").pathString
        val qux_klib = libsDir.resolve("qux.klib").pathString

        with(Path(generateNewKlib(asFile = false, fileExtension = ""))) {
            copyToRecursively(Path(foo), followLinks = false, overwrite = false)
            copyToRecursively(Path(foo_klib), followLinks = false, overwrite = false)
            copyToRecursively(Path(bar), followLinks = false, overwrite = false)
            copyToRecursively(Path(baz_klib), followLinks = false, overwrite = false)
        }

        with(Path(generateNewKlib(asFile = true, fileExtension = "klib"))) {
            copyToRecursively(Path(bar_klib), followLinks = false, overwrite = false)
            copyToRecursively(Path(baz), followLinks = false, overwrite = false)
            copyToRecursively(Path(qux), followLinks = false, overwrite = false)
            copyToRecursively(Path(qux_klib), followLinks = false, overwrite = false)
        }

        assertEquals(
            listOf("bar", "bar.klib", "baz", "baz.klib", "foo", "foo.klib", "qux", "qux.klib"),
            libsDir.listDirectoryEntries().map { it.name }.sorted()
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
        assertTrue(lib.startsWith(tmpDir))

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
        val manifestFile = Path(libraryPath).resolve("default/manifest")
        with(manifestFile.readProperties()) {
            assertTrue(containsKey(KLIB_PROPERTY_ABI_VERSION))
            remove(KLIB_PROPERTY_ABI_VERSION)
            assertFalse(containsKey(KLIB_PROPERTY_ABI_VERSION))
            manifestFile.writeProperties(this)
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

    @Test
    fun testMultipleLibraryProviders() {
        val a = generateNewKlib(asFile = false, fileExtension = "")
        val b = generateNewKlib(asFile = true, fileExtension = "klib")

        val allPaths = buildList {
            add(a)
            addAll(corruptedLibraryPaths)
            add(stdlib)
            addAll(invalidPaths)
            add(b)
        }

        // Load libraries through `libraryPaths`.
        KlibLoader {
            libraryPaths(allPaths)
        }.load()
            .assertLoadedLibraries(stdlib, a, b)
            .assertProblematicLibraries(
                notFoundPaths = invalidPaths,
                invalidFormatPaths = corruptedLibraryPaths
            )

        // Load libraries through a single provider.
        KlibLoader {
            libraryProviders(DefaultKlibLibraryProvider(allPaths))
        }.load()
            .assertLoadedLibraries(stdlib, a, b)
            .assertProblematicLibraries(
                notFoundPaths = invalidPaths,
                invalidFormatPaths = corruptedLibraryPaths
            )

        // Load libraries through a mix of `libraryPaths` and multiple providers.
        KlibLoader {
            libraryProviders(DefaultKlibLibraryProvider(allPaths.shuffled())) // paths are shuffled
            libraryPaths(allPaths) // paths provided through `libraryPaths` are loaded first
            libraryProviders(DefaultKlibLibraryProvider(allPaths.shuffled())) // paths are shuffled
        }.load()
            .assertLoadedLibraries(stdlib, a, b)
            .assertProblematicLibraries(
                notFoundPaths = invalidPaths,
                invalidFormatPaths = corruptedLibraryPaths
            )
    }

    @Test
    fun testNewCompanionInitializationRead() {
        val klib = generateNewKlib(
            asFile = true,
            fileExtension = "",
            withCompanionBlocksAndExtensionsFeature = true,
        )
        val lib: Path = Paths.get(klib)

        val loaded = KlibLoader {
            libraryPaths(lib)
        }.load()

        loaded.librariesStdlibFirst.forEach {
            assertTrue(it.newCompanionInitializationEnabled)
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
            val canonicalLibraryPath: String = Path(libraryPath).toRealPath().pathString

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

    private fun generateNewKlib(
        asFile: Boolean,
        fileExtension: String,
        abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
        withCompanionBlocksAndExtensionsFeature: Boolean = false,
    ): String {
        val uid = (generatedLibsCounter++).toString().padStart(3, '0')
        val baseName = "klib-as_${if (asFile) "file" else "dir"}-ext_$fileExtension-$uid"

        val sourceFile = tmpDir.resolve("$baseName.kt")
        sourceFile.writeText("private fun f() = Unit")

        val klibLocation = tmpDir.resolve(if (fileExtension.isNotEmpty()) "$baseName.$fileExtension" else baseName)
        assertFalse(klibLocation.exists()) { "KLIB should not exist before compilation: $klibLocation" }

        compileKlib(
            parameters = CompilationParameters(
                asFile = asFile,
                sourceFile = sourceFile,
                klibLocation = klibLocation,
                abiVersion = abiVersion,
                withCompanionBlocksAndExtensionsFeature = withCompanionBlocksAndExtensionsFeature
            )
        )

        // Sometimes the compiler sets file extension on its own. This needs to be fixed specifically for KLIB loader tests.
        if (asFile && !klibLocation.exists()) {
            val altKlibLocation = klibLocation.resolveSibling(klibLocation.nameWithoutExtension + ".klib")
            if (altKlibLocation.exists()) altKlibLocation.moveTo(klibLocation)
        }

        assertTrue(klibLocation.exists()) { "KLIB should exist after compilation: $klibLocation" }
        assertEquals(fileExtension, klibLocation.extension)

        return klibLocation.pathString
    }

    data class CompilationParameters(
        val asFile: Boolean,
        val sourceFile: Path,
        val klibLocation: Path,
        val abiVersion: KotlinAbiVersion,
        val withCompanionBlocksAndExtensionsFeature: Boolean = false,
    )

    protected abstract fun compileKlib(
        parameters: CompilationParameters
    )
}
