/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import com.intellij.openapi.util.io.FileUtil.createTempDirectory
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.writer.KlibWriter
import org.jetbrains.kotlin.library.writer.asComponentWriters
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File

@OptIn(ExperimentalLibraryAbiReader::class)
class ManifestReadingTest {
    private lateinit var buildDir: File

    @BeforeEach
    fun setUp(testInfo: TestInfo) {
        buildDir = createTempDirectory(testInfo.testClass.get().simpleName, testInfo.testMethod.get().name)
    }

    @AfterEach
    fun tearDown() {
        buildDir.deleteRecursively()
    }

    @Test
    fun testManifestReading() {
        val testData = mapOf(
            "sample-library-1" to LibraryManifest(
                platform = BuiltInsPlatform.JS.name,
                platformTargets = emptyList(),
                compilerVersion = "1.23.45",
                abiVersion = "2.34.56",
                irProviderName = "test_ir_provider_123"
            ),
            "sample-library-2" to LibraryManifest(
                platform = BuiltInsPlatform.NATIVE.name,
                platformTargets = listOf(
                    LibraryTarget.Native("ios_arm64"),
                    LibraryTarget.Native("ios_simulator_arm64"),
                    LibraryTarget.Native("macos_arm64"),
                    LibraryTarget.Native("macos_x64"),
                ),
                compilerVersion = null,
                abiVersion = null,
                irProviderName = null
            ),
            "sample-library-3" to LibraryManifest(
                platform = BuiltInsPlatform.WASM.name,
                platformTargets = listOf(
                    LibraryTarget.WASM("wasm-js"),
                    LibraryTarget.WASM("wasm-wasi"),
                ),
                compilerVersion = null,
                abiVersion = null,
                irProviderName = null
            ),
        )

        testData.forEach { (libraryName, originalManifest) ->
            val libraryFile = createEmptyLibraryWithSpecificManifest(libraryName, originalManifest)
            val readManifest = LibraryAbiReader.readAbiInfo(libraryFile).manifest

            assertTrue(originalManifest !== readManifest) { "Library name: $libraryName" }
            assertEquals(originalManifest, readManifest) { "Library name: $libraryName" }
        }
    }

    private fun createEmptyLibraryWithSpecificManifest(libraryName: String, libraryManifest: LibraryManifest): File {
        val libraryFile = buildDir.resolve("$libraryName.klib")

        val platform = libraryManifest.platform?.let(BuiltInsPlatform::parseFromString)
            ?: error("Unknown platform: ${libraryManifest.platform}")

        val targetNames = when (platform) {
            BuiltInsPlatform.NATIVE -> libraryManifest.platformTargets.filterIsInstance<LibraryTarget.Native>().map { it.name }
            BuiltInsPlatform.WASM -> libraryManifest.platformTargets.filterIsInstance<LibraryTarget.WASM>().map { it.name }
            else -> emptyList()
        }

        KlibWriter {
            manifest {
                moduleName(libraryName)
                versions(
                    KotlinLibraryVersioning(
                        compilerVersion = libraryManifest.compilerVersion,
                        abiVersion = libraryManifest.abiVersion?.parseKotlinAbiVersion(),
                        metadataVersion = null
                    )
                )
                platformAndTargets(platform, targetNames)
                customProperties {
                    libraryManifest.irProviderName?.let { irProviderName ->
                        this[KLIB_PROPERTY_IR_PROVIDER] = irProviderName
                    }
                }
            }
            include(
                SerializedIrModule(files = emptyList(), fileWithPreparedInlinableFunctions = null).asComponentWriters(), // empty IR
            )
        }.writeTo(libraryFile.absolutePath)

        return libraryFile
    }
}
