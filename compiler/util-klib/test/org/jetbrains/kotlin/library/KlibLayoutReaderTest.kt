/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.konan.file.createTempDir
import org.jetbrains.kotlin.konan.file.zipDirAs
import org.jetbrains.kotlin.library.TestComponentConstants.MANDATORY_COMPONENT_BASE_FOLDER_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.MANDATORY_COMPONENT_INT_VALUE_FILE_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_BASE_FOLDER_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME
import org.jetbrains.kotlin.library.impl.KlibComponentsBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.io.IOException
import java.util.Collections.rotate
import java.util.UUID
import kotlin.random.Random
import org.jetbrains.kotlin.konan.file.File as KlibFile

class KlibLayoutReaderTest {
    private lateinit var tmpDir: KlibFile
    private val random = Random(System.nanoTime())

    @BeforeEach
    fun setup(info: TestInfo) {
        tmpDir = createTempDir(info.testClass.get().simpleName + "-" + info.testMethod.get().name)
    }

    @AfterEach
    fun tearDown() {
        tmpDir.deleteRecursively()
    }

    @Test
    fun `Test reading data and extracting files from plain and compressed libraries`() {
        repeat(10) { iterationNr ->
            val color = getRandomColor()
            val furnitureItems = getRandomFurnitureItems(count = iterationNr)

            fun checkLibrary(lib: TestLib, isFileExtractionExpected: Boolean) {
                assertEquals(iterationNr, lib.mandatoryComponent.intValue)

                val optionalComponent = lib.optionalComponent
                    ?: fail("Optional component should be present: $lib")

                assertEquals(color, optionalComponent.stringValue)

                val someUsefulFiles = optionalComponent.pathsOfExtractedFiles.map { KlibFile(it) }
                assertEquals(furnitureItems, someUsefulFiles.mapTo(hashSetOf()) { it.name })

                someUsefulFiles.forEach { file ->
                    val fileWasExtracted = !file.absolutePath.startsWith(lib.location.absolutePath)
                    assertEquals(isFileExtractionExpected, fileWasExtracted)
                    assertEquals("$color ${file.name}", file.readText())
                }
            }

            val klibDir = generateNewPlainKlib(
                intValue = iterationNr,
                stringValue = color,
                fileNames = furnitureItems,
            )
            checkLibrary(klibDir.toTestLib(), isFileExtractionExpected = false)

            val klibFile = klibDir.compress()
            checkLibrary(klibFile.toTestLib(), isFileExtractionExpected = true)
        }
    }

    @Test
    fun `Test accessing unavailable mandatory component`() {
        val klibDir = generateNewPlainKlib(intValue = 42, stringValue = "fortyTwo", fileNames = emptyList())

        assertEquals(42, klibDir.toTestLib().mandatoryComponent.intValue)
        assertEquals(42, klibDir.compress().toTestLib().mandatoryComponent.intValue)

        TestMandatoryComponentLayout(klibDir).baseDir.deleteRecursively()

        with(klibDir.toTestLib().mandatoryComponent) { assertThrows<IOException> { intValue } }
        with(klibDir.compress().toTestLib().mandatoryComponent) { assertThrows<IOException> { intValue } }
    }

    @Test
    fun `Test accessing unavailable optional component`() {
        val klibDir = generateNewPlainKlib(intValue = 42, stringValue = "fortyTwo", fileNames = emptyList())

        assertEquals("fortyTwo", klibDir.toTestLib().optionalComponent?.stringValue)
        assertEquals("fortyTwo", klibDir.compress().toTestLib().optionalComponent?.stringValue)

        TestOptionalComponentLayout(klibDir).baseDir.deleteRecursively()

        assertEquals(null, klibDir.toTestLib().optionalComponent)
        assertEquals(null, klibDir.compress().toTestLib().optionalComponent)
    }

    private fun generateNewPlainKlib(intValue: Int, stringValue: String, fileNames: Collection<String>): KlibFile {
        val klibDir = tmpDir.child(UUID.randomUUID().toString())

        val mandatoryComponentLayout = TestMandatoryComponentLayout(klibDir)
        mandatoryComponentLayout.baseDir.mkdirs()
        mandatoryComponentLayout.intValueFile.writeText("$intValue")

        val optionalComponentLayout = TestOptionalComponentLayout(klibDir)
        optionalComponentLayout.baseDir.mkdirs()
        optionalComponentLayout.stringValueFile.writeText(stringValue)
        optionalComponentLayout.extractedFilesDir.mkdirs()
        for (fileName in fileNames) {
            optionalComponentLayout.extractedFilesDir.child(fileName).writeText("$stringValue $fileName")
        }

        return klibDir
    }

    private fun KlibFile.compress(): KlibFile {
        val klibFile = KlibFile("$absolutePath.klib")
        if (klibFile.exists) klibFile.delete()
        zipDirAs(klibFile)
        return klibFile
    }

    private fun KlibFile.toTestLib(): TestLib = TestLib(this)

    private fun getRandomColor(): String = COLORS.random(random)

    private fun getRandomFurnitureItems(count: Int): Set<String> {
        require(count >= 0 && count <= FURNITURE_ITEMS.size)
        if (count == 0) return emptySet()
        return FURNITURE_ITEMS.toMutableList().also { rotate(it, random.nextInt(count)) }.take(count).toSet()
    }

    private companion object {
        // Collections of some useful words.
        val COLORS = listOf("red", "orange", "yellow", "green", "blue", "purple", "pink", "brown", "black", "gray", "white")
        val FURNITURE_ITEMS = listOf("chair", "sofa", "table", "bed", "cabinet", "bench", "desk", "drawer", "wardrobe", "cupboard")
    }
}

private class TestLib(val location: KlibFile) {
    private val layoutReaderFactory = KlibLayoutReaderFactory(location, ZipFileSystemInPlaceAccessor)

    private val components: Map<KlibComponent.Kind<*>, KlibComponent> = KlibComponentsBuilder(layoutReaderFactory = layoutReaderFactory)
        .withMandatory(TestMandatoryComponent.Kind, ::TestMandatoryComponentLayout, ::TestMandatoryComponentImpl)
        .withOptional(TestOptionalComponent.Kind, ::TestOptionalComponentLayout, ::TestOptionalComponentImpl)
        .build()

    val mandatoryComponent: TestMandatoryComponent
        get() = components[TestMandatoryComponent.Kind] as TestMandatoryComponent?
            ?: fail("Mandatory component should be present: $location")

    val optionalComponent: TestOptionalComponent?
        get() = components[TestOptionalComponent.Kind] as TestOptionalComponent?

    override fun toString() = location.absolutePath
}

private interface TestOptionalComponent : KlibOptionalComponent {
    val stringValue: String
    val pathsOfExtractedFiles: Collection<String>

    companion object Kind : KlibOptionalComponent.Kind<TestOptionalComponent, TestOptionalComponentLayout> {
        override fun shouldComponentBeRegistered(layoutReader: KlibLayoutReader<TestOptionalComponentLayout>) =
            layoutReader.readInPlaceOrFallback(false) { it.baseDir.exists }
    }
}

private class TestOptionalComponentImpl(private val layoutReader: KlibLayoutReader<TestOptionalComponentLayout>) : TestOptionalComponent {
    override val stringValue: String by lazy {
        layoutReader.readInPlace { it.stringValueFile.readText() }
    }

    override val pathsOfExtractedFiles: Collection<String> by lazy {
        layoutReader.readExtractingToTemp { it.extractedFilesDir }.listFiles.map(KlibFile::absolutePath)
    }
}

private class TestOptionalComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    val baseDir: KlibFile get() = root.child(OPTIONAL_COMPONENT_BASE_FOLDER_NAME)
    val stringValueFile: KlibFile get() = baseDir.child(OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME)
    val extractedFilesDir: KlibFile get() = baseDir.child(OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME)
}

private interface TestMandatoryComponent : KlibMandatoryComponent {
    val intValue: Int

    companion object Kind : KlibMandatoryComponent.Kind<TestMandatoryComponent>
}

private class TestMandatoryComponentImpl(private val layoutReader: KlibLayoutReader<TestMandatoryComponentLayout>) : TestMandatoryComponent {
    override val intValue: Int by lazy {
        layoutReader.readInPlace { it.intValueFile.readText().toInt() }
    }
}

private class TestMandatoryComponentLayout(root: KlibFile) : KlibComponentLayout(root) {
    val baseDir: KlibFile get() = root.child(MANDATORY_COMPONENT_BASE_FOLDER_NAME)
    val intValueFile: KlibFile get() = baseDir.child(MANDATORY_COMPONENT_INT_VALUE_FILE_NAME)
}


private object TestComponentConstants {
    const val OPTIONAL_COMPONENT_BASE_FOLDER_NAME = "optionalComponentBaseDir"

    const val OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME = "string.txt"
    const val OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME = "filesToBeExtracted"

    const val MANDATORY_COMPONENT_BASE_FOLDER_NAME = "mandatoryComponentBaseDir"

    const val MANDATORY_COMPONENT_INT_VALUE_FILE_NAME = "int.txt"
}

private fun KlibFile.readText(): String = readBytes().toString(Charsets.UTF_8).trimEnd()
