/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.io.zipDirAs
import org.jetbrains.kotlin.library.TestComponentConstants.MANDATORY_COMPONENT_BASE_FOLDER_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.MANDATORY_COMPONENT_INT_VALUE_FILE_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_BASE_FOLDER_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME
import org.jetbrains.kotlin.library.TestComponentConstants.OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME
import org.jetbrains.kotlin.library.impl.KlibComponentsCache
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.io.IOException
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.Collections.rotate
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readBytes
import kotlin.io.path.writeText
import kotlin.random.Random

@OptIn(ExperimentalPathApi::class)
class KlibLayoutReaderTest {
    private lateinit var tmpDir: Path
    private val random = Random(System.nanoTime())

    @BeforeEach
    fun setup(info: TestInfo) {
        tmpDir = createTempDirectory(info.testClass.get().simpleName + "-" + info.testMethod.get().name)
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

                val someUsefulFiles = optionalComponent.pathsOfExtractedFiles.map { Path(it) }
                assertEquals(furnitureItems, someUsefulFiles.mapTo(hashSetOf()) { it.name })

                someUsefulFiles.forEach { file ->
                    val fileWasExtracted = !file.absolutePathString().startsWith(lib.location.absolutePathString())
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

    private fun generateNewPlainKlib(intValue: Int, stringValue: String, fileNames: Collection<String>): Path {
        val klibDir = tmpDir.resolve(UUID.randomUUID().toString())

        val mandatoryComponentLayout = TestMandatoryComponentLayout(klibDir)
        mandatoryComponentLayout.baseDir.createDirectories()
        mandatoryComponentLayout.intValueFile.writeText("$intValue")

        val optionalComponentLayout = TestOptionalComponentLayout(klibDir)
        optionalComponentLayout.baseDir.createDirectories()
        optionalComponentLayout.stringValueFile.writeText(stringValue)
        optionalComponentLayout.extractedFilesDir.createDirectories()
        for (fileName in fileNames) {
            optionalComponentLayout.extractedFilesDir.resolve(fileName).writeText("$stringValue $fileName")
        }

        return klibDir
    }

    private fun Path.compress(): Path {
        val klibFile = Path("${absolutePathString()}.klib")
        klibFile.deleteIfExists()
        zipDirAs(klibFile)
        return klibFile
    }

    private fun Path.toTestLib(): TestLib = TestLib(this)

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

private class TestLib(val location: Path) {
    private val layoutReaderFactory = KlibLayoutReaderFactory(location, ZipFileSystemInPlaceAccessor)
    private val components = KlibComponentsCache(layoutReaderFactory)

    val mandatoryComponent: TestMandatoryComponent
        get() = components.getComponent(TestMandatoryComponent.Kind)
            ?: fail("Mandatory component should be present: $location")

    val optionalComponent: TestOptionalComponent?
        get() = components.getComponent(TestOptionalComponent.Kind)

    override fun toString() = location.absolutePathString()
}

/**
 * Note: "Optional" means that the actual data availability check is performed in
 * [TestOptionalComponent.Kind.createComponentIfDataInKlibIsAvailable]: If there is no data to be read by the component,
 * no component instance is created and `null` is returned.
 */
private interface TestOptionalComponent : KlibComponent {
    val stringValue: String
    val pathsOfExtractedFiles: Collection<String>

    companion object Kind : KlibComponent.Kind<TestOptionalComponent, TestOptionalComponentLayout> {
        override fun createLayout(root: Path) = TestOptionalComponentLayout(root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<TestOptionalComponentLayout>) =
            if (layoutReader.readInPlaceOrFallback(false) { it.baseDir.exists() }) TestOptionalComponentImpl(layoutReader) else null
    }
}

private class TestOptionalComponentImpl(private val layoutReader: KlibLayoutReader<TestOptionalComponentLayout>) : TestOptionalComponent {
    override val stringValue: String by lazy {
        layoutReader.readInPlace { it.stringValueFile.readText() }
    }

    override val pathsOfExtractedFiles: Collection<String> by lazy {
        layoutReader.readExtractingToTemp { it.extractedFilesDir }.listDirectoryEntries().map(Path::absolutePathString)
    }
}

private class TestOptionalComponentLayout(root: Path) : KlibComponentLayout(root) {
    val baseDir: Path get() = root.resolve(OPTIONAL_COMPONENT_BASE_FOLDER_NAME)
    val stringValueFile: Path get() = baseDir.resolve(OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME)
    val extractedFilesDir: Path get() = baseDir.resolve(OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME)
}

/**
 * Note: "Mandatory" means that no data availability check is performed in
 * [TestMandatoryComponent.Kind.createComponentIfDataInKlibIsAvailable]: The component instance is always created.
 * This can lead to some IO errors later during an attempt to read something using the component if no corresponding
 * data is available in the KLIB.
 */
private interface TestMandatoryComponent : KlibComponent {
    val intValue: Int

    companion object Kind : KlibComponent.Kind<TestMandatoryComponent, TestMandatoryComponentLayout> {
        override fun createLayout(root: Path) = TestMandatoryComponentLayout(root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<TestMandatoryComponentLayout>) =
            TestMandatoryComponentImpl(layoutReader)
    }
}

private class TestMandatoryComponentImpl(private val layoutReader: KlibLayoutReader<TestMandatoryComponentLayout>) : TestMandatoryComponent {
    override val intValue: Int by lazy {
        layoutReader.readInPlace { it.intValueFile.readText().toInt() }
    }
}

private class TestMandatoryComponentLayout(root: Path) : KlibComponentLayout(root) {
    val baseDir: Path get() = root.resolve(MANDATORY_COMPONENT_BASE_FOLDER_NAME)
    val intValueFile: Path get() = baseDir.resolve(MANDATORY_COMPONENT_INT_VALUE_FILE_NAME)
}


private object TestComponentConstants {
    const val OPTIONAL_COMPONENT_BASE_FOLDER_NAME = "optionalComponentBaseDir"

    const val OPTIONAL_COMPONENT_STRING_VALUE_FILE_NAME = "string.txt"
    const val OPTIONAL_COMPONENT_EXTRACTED_FILES_FOLDER_NAME = "filesToBeExtracted"

    const val MANDATORY_COMPONENT_BASE_FOLDER_NAME = "mandatoryComponentBaseDir"

    const val MANDATORY_COMPONENT_INT_VALUE_FILE_NAME = "int.txt"
}

private fun Path.readText(): String = readBytes().toString(Charsets.UTF_8).trimEnd()
