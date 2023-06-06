/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

@OptIn(ExperimentalLibraryAbiReader::class)
class ManifestRenderingTest {
    private lateinit var buildDir: File
    private lateinit var sourceFile: File

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        buildDir = setUpBuildDir(testInfo)
        sourceFile = buildDir.resolve("source.kt").apply { createNewFile() } // Just an empty file.
    }

    @Test
    fun renderWithManifest() {
        val customManifest = LibraryManifest(
            platform = generateRandomString(10),
            nativeTargets = List(5) { index -> "${index}_${generateRandomString(8)}" },
            compilerVersion = generateRandomVersion(),
            abiVersion = generateRandomVersion(),
            libraryVersion = generateRandomVersion(),
            irProviderName = generateRandomString(20)
        )

        val libraryFile = buildLibrary(sourceFile, libraryName = "sample-library", buildDir)
        patchManifest(libraryFile, customManifest)

        val readManifest = LibraryAbiReader.readAbiInfo(libraryFile).manifest
        assertTrue(customManifest !== readManifest)
        assertEquals(customManifest, readManifest)
    }
}
