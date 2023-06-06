/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
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
            platform = BuiltInsPlatform.JS.name,
            nativeTargets = listOf("ios_arm64", "ios_simulator_arm64", "macos_arm64", "macos_x64"),
            compilerVersion = "1.23.45",
            abiVersion = "2.34.56",
            libraryVersion = "3.45.67",
            irProviderName = "test_ir_provider_123"
        )

        val libraryFile = buildLibrary(sourceFile, libraryName = "sample-library", buildDir)
        patchManifest(libraryFile, customManifest)

        val readManifest = LibraryAbiReader.readAbiInfo(libraryFile).manifest
        assertTrue(customManifest !== readManifest)
        assertEquals(customManifest, readManifest)
    }
}
