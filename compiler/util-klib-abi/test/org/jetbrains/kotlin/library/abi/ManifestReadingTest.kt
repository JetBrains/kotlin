/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import com.intellij.openapi.util.io.FileUtil.createTempDirectory
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.*
import org.jetbrains.kotlin.konan.file.File as KFile

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
                nativeTargets = emptyList(),
                compilerVersion = "1.23.45",
                abiVersion = "2.34.56",
                libraryVersion = "3.45.67",
                irProviderName = "test_ir_provider_123"
            ),
            "sample-library-2" to LibraryManifest(
                platform = BuiltInsPlatform.NATIVE.name,
                nativeTargets = listOf("ios_arm64", "ios_simulator_arm64", "macos_arm64", "macos_x64"),
                compilerVersion = null,
                abiVersion = null,
                libraryVersion = null,
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
        val libraryVersioning = KotlinLibraryVersioning(
            libraryVersion = libraryManifest.libraryVersion,
            compilerVersion = libraryManifest.compilerVersion,
            abiVersion = libraryManifest.abiVersion?.parseKotlinAbiVersion(),
            metadataVersion = null
        )
        val builtInsPlatform = libraryManifest.platform?.let(BuiltInsPlatform::parseFromString)
            ?: error("Unknown platform: ${libraryManifest.platform}")

        val libraryFile = buildDir.resolve("$libraryName.klib")
        val libraryKFile = KFile(libraryFile.absolutePath)

        val libraryLayout = KotlinLibraryLayoutForWriter(libraryKFile, libraryKFile)
        val library = KotlinLibraryWriterImpl(
            moduleName = libraryName,
            versions = libraryVersioning,
            builtInsPlatform = builtInsPlatform,
            nativeTargets = libraryManifest.nativeTargets,
            nopack = true,
            shortName = libraryName,
            layout = libraryLayout
        )
        libraryManifest.irProviderName?.let { irProviderName ->
            library.addManifestAddend(
                Properties().apply {
                    this[KLIB_PROPERTY_IR_PROVIDER] = irProviderName
                }
            )
        }
        library.addIr(SerializedIrModule(files = emptyList())) // Empty library.
        library.commit()

        return libraryFile
    }
}
