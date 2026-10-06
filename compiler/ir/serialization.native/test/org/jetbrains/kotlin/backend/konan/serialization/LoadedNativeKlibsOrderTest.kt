/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KLIB_PROPERTY_NATIVE_TARGETS
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.manifest
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString

/**
 * Pins the order of [LoadedNativeKlibs.all]. Properties pinned below:
 * The order:
 * - stdlib is always first, wherever it was requested from,
 * - `-l`/`-library` batch in CLI order
 * - `-Xinclude` batch in CLI order
 * - `-Xadd-cache`
 * - the implicitly loaded platform libraries, sorted by name.
 *
 * A library named more than once keeps the position of its FIRST mention (e.g a library passed as both
 * `-l` and `-Xinclude` stays in the `-l` bucket)
 *
 * `-Xexport-library` and `-friend-modules` contribute nothing to the order: they only select from the
 * libraries loaded via the buckets above.
 */
class LoadedNativeKlibsOrderTest {
    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `stdlib first, then -library, then -Xinclude, then -Xadd-cache, then platform libs`() {
        val nativeHome = emulateNativeDistribution(platformLibs = listOf("posix", "accelerate", "zlib"))

        val loaded = loadNativeKlibs(
            configuration(nativeHome) {
                konanLibraries = listOf(klib("regularB"), klib("regularA"))
                konanIncludedLibraries = listOf(klib("includedB"), klib("includedA"))
                konanLibraryToAddToCache = klib("cached")
                // `konanLibraries` is what the loader is actually given; the CLI assembles it as
                // `-library` ++ `-Xinclude` ++ `-Xadd-cache`. Reproduce that here.
                konanLibraries = konanLibraries + konanIncludedLibraries + listOfNotNull(konanLibraryToAddToCache)
            },
            nativeTarget = TEST_TARGET,
        )

        assertEquals(
            listOf(
                "stdlib",
                "regularB", "regularA",      // -library, argument order (deliberately not alphabetical)
                "includedB", "includedA",    // -Xinclude, argument order
                "cached",                    // -Xadd-cache
                // platform libraries come last, sorted by name(NOT in the order they sit on disk)
                "${PLATFORM_LIB_PREFIX}accelerate",
                "${PLATFORM_LIB_PREFIX}posix",
                "${PLATFORM_LIB_PREFIX}zlib",
            ),
            loaded.all.map { it.uniqueName }
        )
    }

    @Test
    fun `a library mentioned twice keeps the position of its first mention`() {
        val nativeHome = emulateNativeDistribution(platformLibs = emptyList())
        val shared = klib("shared")

        val loaded = loadNativeKlibs(
            configuration(nativeHome) {
                // `shared` is both a regular and an included library. The CLI puts `-library` first, so the
                // deduplication in KlibLoader must leave `shared` in the `-library` bucket, before `regular`.
                konanIncludedLibraries = listOf(shared)
                konanLibraries = listOf(shared, klib("regular")) + konanIncludedLibraries
            },
            nativeTarget = TEST_TARGET,
        )

        assertEquals(listOf("stdlib", "shared", "regular"), loaded.all.map { it.uniqueName })
    }

    @Test
    fun `a library named by several different paths keeps the position of its first mention`() {
        val nativeHome = emulateNativeDistribution(platformLibs = emptyList())
        val first = klib("first")
        val shared = klib("shared")
        val last = klib("last")
        val sharedPath = Path(shared)

        // The same library, spelled three more ways. `KlibLoader` keys its deduplication on
        // `Path.toRealPath()`, which both normalizes and resolves symlinks, so every spelling collapses onto
        // the same entry and the FIRST one decides the position: `shared` has to stay between `first` and
        // `last` instead of moving to wherever it was last mentioned.
        val viaRedundantSegments = sharedPath.parent.resolve(".").resolve(sharedPath.fileName).pathString
        // Both of these can legitimately be unavailable -- creating a symlink may be denied, and a path
        // relative to the working directory does not exist if it sits on another filesystem root -- so each
        // is contributed only when it could be built. The ones that remain still pin the behaviour.
        val viaSymlink = runCatching {
            Files.createSymbolicLink(tempDir.resolve("link-to-shared"), sharedPath).pathString
        }.getOrNull()
        val viaRelativePath = runCatching {
            Path("").toAbsolutePath().relativize(sharedPath).pathString
        }.getOrNull()

        val loaded = loadNativeKlibs(
            configuration(nativeHome) {
                konanLibraries = listOfNotNull(first, shared, last, viaRedundantSegments, viaSymlink, viaRelativePath)
            },
            nativeTarget = TEST_TARGET,
        )

        assertEquals(listOf("stdlib", "first", "shared", "last"), loaded.all.map { it.uniqueName })
    }

    @Test
    fun `exported and friend libraries do not affect the order`() {
        val nativeHome = emulateNativeDistribution(platformLibs = emptyList())
        val a = klib("a")
        val b = klib("b")
        val c = klib("c")

        fun load(exported: List<String>, friends: List<String>) = loadNativeKlibs(
            configuration(nativeHome) {
                konanLibraries = listOf(a, b, c)
                exportedLibraries = exported
                konanFriendLibraries = friends
            },
            nativeTarget = TEST_TARGET,
        ).all.map { it.uniqueName }

        // Selecting different subsets as exported/friends must not reorder anything.
        val expected = listOf("stdlib", "a", "b", "c")
        assertEquals(expected, load(exported = emptyList(), friends = emptyList()))
        assertEquals(expected, load(exported = listOf(c, a), friends = listOf(b)))
        assertEquals(expected, load(exported = listOf(b), friends = listOf(c, a)))
    }

    @Test
    fun `stdlib is hoisted to the first position even when named last`() {
        val nativeHome = emulateNativeDistribution(platformLibs = emptyList())

        val loaded = loadNativeKlibs(
            configuration(nativeHome) {
                // Pass the distribution's own stdlib explicitly, and last. The loader hoists it regardless:
                // stdlib must be deserialized first to set up built-ins.
                konanLibraries = listOf(klib("a"), nativeHome.resolve("klib/common/stdlib").pathString)
            },
            nativeTarget = TEST_TARGET,
        )

        assertEquals(listOf("stdlib", "a"), loaded.all.map { it.uniqueName })
    }

    // -----------------------------------------------------------------------------------------------------

    @OptIn(CompilerConfiguration.Internals::class) // constructing a bare configuration is the point of the test
    private fun configuration(nativeHome: Path, init: CompilerConfiguration.() -> Unit): CompilerConfiguration =
        CompilerConfiguration().apply {
            konanHome = nativeHome.pathString
            // Mock KLIBs carry no compiler version, which the special compatibility checker would reject.
            skipLibrarySpecialCompatibilityChecks = true
            duplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.DENY
            init()
        }

    private fun emulateNativeDistribution(platformLibs: List<String>): Path {
        val distDir = tempDir.resolve("kotlin-native-dist")
        distDir.resolve("klib/common").createMockKlib("stdlib")
        with(distDir.resolve("klib/platform/${TEST_TARGET.name}")) {
            createDirectories()
            platformLibs.forEach { createMockKlib("$PLATFORM_LIB_PREFIX$it") }
        }
        return distDir
    }

    /** Creates a mock KLIB outside the distribution and returns its path, for use as a CLI argument. */
    private fun klib(name: String): String =
        tempDir.resolve("libraries").createMockKlib(name).pathString

    private fun Path.createMockKlib(name: String): Path {
        createDirectories()
        return mockKlib(resolve(name)) {
            manifest(
                uniqueName = name,
                builtInsPlatform = BuiltInsPlatform.NATIVE,
                versioning = KotlinLibraryVersioning(
                    compilerVersion = null,
                    abiVersion = KotlinAbiVersion.CURRENT,
                    metadataVersion = MetadataVersion.INSTANCE,
                ),
                other = { this[KLIB_PROPERTY_NATIVE_TARGETS] = TEST_TARGET.name },
            )
        }
    }

    private companion object {
        val TEST_TARGET = KonanTarget.MACOS_ARM64
        const val PLATFORM_LIB_PREFIX = "org.jetbrains.kotlin.native.platform."
    }
}
