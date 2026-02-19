/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.library.KlibMockDSL.Companion.generateRandomName
import org.jetbrains.kotlin.library.KlibMockDSL.Companion.mockKlib
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NativeKlibPlatformCheckerTest {
    @TempDir
    private lateinit var tmpDir: File

    @Test
    fun `No or empty 'native_targets', no or empty 'commonizer_native_targets'`() {
        listOf(
            mockKlib(nativeTargets = null, nativeCommonizerTargets = null),
            mockKlib(nativeTargets = emptyList(), nativeCommonizerTargets = null),
            mockKlib(nativeTargets = null, nativeCommonizerTargets = emptyList()),
            mockKlib(nativeTargets = emptyList(), nativeCommonizerTargets = emptyList()),
        ).forEach { klib ->
            klib.assertLoadedWith(KlibPlatformChecker.Native())

            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_a"))
            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_b"))
            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_c"))

            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_a"))
            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_b"))
            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_c"))
        }
    }

    @Test
    fun `Non-empty 'native_targets', no or empty 'commonizer_native_targets'`() {
        listOf(
            mockKlib(nativeTargets = listOf("target_a", "target_b"), nativeCommonizerTargets = null),
            mockKlib(nativeTargets = listOf("target_a", "target_b"), nativeCommonizerTargets = emptyList()),
        ).forEach { klib ->
            klib.assertLoadedWith(KlibPlatformChecker.Native())

            klib.assertLoadedWith(KlibPlatformChecker.Native("target_a"))
            klib.assertLoadedWith(KlibPlatformChecker.Native("target_b"))
            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_c"))

            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_a"))
            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_b"))
            klib.assertNotLoadedWith(KlibPlatformChecker.NativeMetadata("target_c"))
        }
    }

    @Test
    fun `No or empty 'native_targets', non-empty 'commonizer_native_targets'`() {
        listOf(
            mockKlib(nativeTargets = null, nativeCommonizerTargets = listOf("target_a", "target_b")),
            mockKlib(nativeTargets = emptyList(), nativeCommonizerTargets = listOf("target_a", "target_b")),
        ).forEach { klib ->
            klib.assertLoadedWith(KlibPlatformChecker.Native())

            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_a"))
            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_b"))
            klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_c"))

            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_a"))
            klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_b"))
            klib.assertNotLoadedWith(KlibPlatformChecker.NativeMetadata("target_c"))
        }
    }

    @Test
    fun `Non-empty 'native_targets', non-empty 'commonizer_native_targets'`() {
        val klib = mockKlib(nativeTargets = listOf("target_a", "target_b"), nativeCommonizerTargets = listOf("target_c", "target_d"))

        klib.assertLoadedWith(KlibPlatformChecker.Native())

        klib.assertLoadedWith(KlibPlatformChecker.Native("target_a"))
        klib.assertLoadedWith(KlibPlatformChecker.Native("target_b"))
        klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_c"))
        klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_d"))
        klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_e"))

        klib.assertNotLoadedWith(KlibPlatformChecker.NativeMetadata("target_a"))
        klib.assertNotLoadedWith(KlibPlatformChecker.NativeMetadata("target_b"))
        klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_c"))
        klib.assertLoadedWith(KlibPlatformChecker.NativeMetadata("target_d"))
        klib.assertNotLoadedWith(KlibPlatformChecker.Native("target_e"))
    }

    private fun mockKlib(
        nativeTargets: List<String>?,
        nativeCommonizerTargets: List<String>?,
    ): File = mockKlib(tmpDir.resolve(generateRandomName(10))) {
        manifest(
            uniqueName = "sample",
            builtInsPlatform = BuiltInsPlatform.NATIVE,
            versioning = KotlinLibraryVersioning(
                compilerVersion = null,
                abiVersion = KotlinAbiVersion.CURRENT,
                metadataVersion = MetadataVersion.INSTANCE,
            )
        ) {
            if (nativeTargets != null) {
                this[KLIB_PROPERTY_NATIVE_TARGETS] = nativeTargets.joinToString(" ")
            }

            if (nativeCommonizerTargets != null) {
                this[KLIB_PROPERTY_COMMONIZER_NATIVE_TARGETS] = nativeCommonizerTargets.joinToString(" ")
            }
        }
    }

    private fun File.assertLoadedWith(checker: KlibPlatformChecker) {
        val result = KlibLoader {
            libraryPaths(this@assertLoadedWith)
            platformChecker(checker)
        }.load()

        assertFalse(result.hasProblems)
        assertEquals(1, result.librariesStdlibFirst.size)
    }

    private fun File.assertNotLoadedWith(checker: KlibPlatformChecker) {
        val result = KlibLoader {
            libraryPaths(this@assertNotLoadedWith)
            platformChecker(checker)
        }.load()

        assertTrue(result.hasProblems)
        assertEquals(1, result.problematicLibraries.size)
        assertTrue(result.problematicLibraries[0].problemCase is KlibLoaderResult.ProblemCase.PlatformCheckMismatch)
        assertEquals(0, result.librariesStdlibFirst.size)
    }
}
