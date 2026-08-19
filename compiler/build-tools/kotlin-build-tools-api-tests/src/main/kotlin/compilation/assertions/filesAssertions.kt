/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ModuleContext
import org.junit.jupiter.api.Assertions.assertEquals
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * Equivalent to [assertNoCompiledSources] with an empty array/set
 */
context(module: ModuleContext)
fun CompilationOutcome.assertNoCompiledSources() {
    assertCompiledSources()
}

context(module: ModuleContext)
fun CompilationOutcome.assertCompiledSources(vararg expectedCompiledSources: String) {
    assertCompiledSources(expectedCompiledSources.toSet())
}

context(module: ModuleContext)
fun CompilationOutcome.assertCompiledSources(expectedCompiledSources: Set<String>) {
    val actualCompiledSources = parseCompilationSteps().flatten().toSet()
    val normalizedPaths = normalizeFileNames(expectedCompiledSources)
    assertEquals(normalizedPaths, actualCompiledSources) {
        """
            Compiled sources do not match. Set diff:
            Unexpected: ${actualCompiledSources - normalizedPaths}
            Missing: ${normalizedPaths - actualCompiledSources}
        
            Full sets:
        """.trimIndent()
    }
}

/**
 * Asserts the per-step compilation sets during incremental compilation.
 * Unlike [assertCompiledSources], this checks each IC iteration separately,
 * which is necessary for verifying monotonous compile set expansion behavior.
 *
 * @param steps each set contains file names expected in the corresponding compile iteration
 */
context(module: ModuleContext)
fun CompilationOutcome.assertCompilationSteps(vararg steps: Set<String>) {
    val actualSteps = parseCompilationSteps()
    val expectedSteps = steps.map { normalizeFileNames(it) }
    assertEquals(expectedSteps.size, actualSteps.size) {
        "Expected ${expectedSteps.size} compilation steps but got ${actualSteps.size}.\nActual steps: $actualSteps"
    }
    expectedSteps.zip(actualSteps).forEachIndexed { index, [expected, actual] ->
        assertEquals(expected, actual) {
            """
                Compilation step ${index + 1} does not match.
                Unexpected: ${actual - expected}
                Missing: ${expected - actual}
            """.trimIndent()
        }
    }
}

context(module: ModuleContext)
private fun normalizeFileNames(fileNames: Set<String>): Set<String> =
    fileNames.map { fileName ->
        module.sourcesDirectory.resolve(fileName)
            .relativeTo(module.project.projectDirectory)
            .toString()
    }.toSet()

private fun CompilationOutcome.parseCompilationSteps(): List<Set<String>> {
    requireLogLevel(LogLevel.DEBUG)
    return logLines.getValue(LogLevel.DEBUG)
        .map { it.removePrefix("[KOTLIN] ") }
        .filter { it.startsWith("compile iteration") }
        .map { line ->
            line.removePrefix("compile iteration: ").trim().split(", ").toSet()
        }
}

/**
 * Asserts that the compiler produces all files declared as expected outputs.
 * Unless there's explicit expected output for the module's Kotlin module files, the default matching [Module.moduleName] will be added automatically.
 */
context(module: ModuleContext)
fun CompilationOutcome.assertOutputs(vararg expectedOutputs: String) {
    assertOutputs(expectedOutputs.toSet())
}

context(module: ModuleContext)
fun CompilationOutcome.assertOutputsContains(vararg expectedOutputs: String) {
    assertOutputs(expectedOutputs.toSet(), doNotFailOnExtraFiles = true)
}

/**
 * Asserts that the compiler produces all files declared as expected outputs.
 * Unless there's explicit expected output for the module's Kotlin module files, the default matching [Module.moduleName] will be added automatically.
 */
context(module: ModuleContext)
fun CompilationOutcome.assertOutputs(expectedOutputs: Set<String>, doNotFailOnExtraFiles: Boolean = false) {
    val filesLeft = expectedOutputs.map { module.outputDirectory.resolve(it).relativeTo(module.outputDirectory) }
        .toMutableSet()
        .apply {
            if (module is JvmModule && none { it.fileName.toString().endsWith(".kotlin_module") }) {
                add(module.outputDirectory.resolve("META-INF/${module.moduleName}.kotlin_module").relativeTo(module.outputDirectory))
            }
        }
    val notDeclaredFiles = hashSetOf<Path>()
    for (file in module.outputDirectory.walk()) {
        if (!file.isRegularFile()) continue
        val currentFile = file.relativeTo(module.outputDirectory)
        filesLeft.remove(currentFile).also { wasPreviously ->
            if (!wasPreviously) notDeclaredFiles.add(currentFile)
        }
    }
    assert(filesLeft.isEmpty() && (doNotFailOnExtraFiles || notDeclaredFiles.isEmpty())) {
        val errors = mutableListOf<String>()
        if (filesLeft.isNotEmpty()) {
            errors.add("The following files were declared as expected, but not actually produced: $filesLeft")
        }
        if (notDeclaredFiles.isNotEmpty()) {
            errors.add("The following files weren't declared as expected output files: $notDeclaredFiles")
        }
        errors.joinToString(separator = "\n")
    }
}

context(module: ModuleContext)
fun assertOutputFileContains(fileName: String, expectedContent: String) {
    val file = module.outputDirectory.resolve(fileName)
    assert(file.exists()) {
        "File $file does not exist.\nOther files in the directory:\n${
            module.outputDirectory.listDirectoryEntries().joinToString("\n")
        }"
    }
    val fileContents = file.readText()
    assert(expectedContent in fileContents) { "File $file does not contain expected content.\n\nFile contents:\n$fileContents" }
}

context(module: ModuleContext)
fun assertOutputFileDoesNotContain(fileName: String, unexpectedContent: String) {
    val file = module.outputDirectory.resolve(fileName)
    assert(file.exists()) {
        "File $file does not exist.\nOther files in the directory:\n${
            module.outputDirectory.listDirectoryEntries().joinToString("\n")
        }"
    }
    val fileContents = file.readText()
    assert(unexpectedContent !in fileContents) {
        "File $file unexpectedly contains \"$unexpectedContent\".\n\nFile contents:\n$fileContents"
    }
}

/**
 * Asserts that a binary output file contains the byte sequence of [expectedContent].
 */
context(module: ModuleContext)
fun assertOutputFileBytesContain(fileName: String, expectedContent: String) {
    val file = module.outputDirectory.resolve(fileName)
    assert(file.exists()) {
        "File $file does not exist.\nOther files in the directory:\n${
            module.outputDirectory.listDirectoryEntries().joinToString("\n")
        }"
    }
    val fileBytes = file.readBytes()
    assert(fileBytes.containsSubsequence(expectedContent.encodeToByteArray())) {
        "Binary file $file does not contain the expected byte sequence \"$expectedContent\"."
    }
}

/**
 * Asserts that a binary output file does not contain the byte sequence of [unexpectedContent].
 */
context(module: ModuleContext)
fun assertOutputFileBytesDoNotContain(fileName: String, unexpectedContent: String) {
    val file = module.outputDirectory.resolve(fileName)
    assert(file.exists()) {
        "File $file does not exist.\nOther files in the directory:\n${
            module.outputDirectory.listDirectoryEntries().joinToString("\n")
        }"
    }
    val fileBytes = file.readBytes()
    assert(!fileBytes.containsSubsequence(unexpectedContent.encodeToByteArray())) {
        "Binary file $file unexpectedly contains the byte sequence \"$unexpectedContent\"."
    }
}

private fun ByteArray.containsSubsequence(subsequence: ByteArray): Boolean {
    if (subsequence.isEmpty()) return true
    if (subsequence.size > size) return false
    outer@ for (start in 0..(size - subsequence.size)) {
        for (i in subsequence.indices) {
            if (this[start + i] != subsequence[i]) continue@outer
        }
        return true
    }
    return false
}

context(module: ModuleContext)
private fun outputFilesWithExtension(extension: String): List<String> =
    module.outputDirectory.walk()
        .filter { it.isRegularFile() && it.fileName.toString().endsWith(extension) }
        .map { it.relativeTo(module.outputDirectory).toString() }
        .toList()

/**
 * Asserts that the output directory contains exactly [expectedCount] regular files whose name ends with [extension].
 */
context(module: ModuleContext)
fun assertOutputFileCountWithExtension(extension: String, expectedCount: Int) {
    val files = outputFilesWithExtension(extension)
    assertEquals(expectedCount, files.size) {
        "Expected $expectedCount file(s) with the extension \"$extension\" in ${module.outputDirectory}, but found ${files.size}: ${files.sorted()}"
    }
}

/*
 * The names below mirror the klib layout constants in `org.jetbrains.kotlin.library.components.KlibMetadataConstants`
 * (metadata-specific names) and `org.jetbrains.kotlin.library.KLIB_MANIFEST_FILE_NAME` / the default klib component
 * directory. Those live in `:compiler:util-klib`, which is intentionally not on the compile classpath of this test
 * module (the tests compile against the BTA API and load the full compiler on a separate runtime classpath), so the
 * relevant names are duplicated here rather than referenced directly.
 */
private const val KLIB_DEFAULT_COMPONENT_DIR = "default"
private const val KLIB_MANIFEST_FILE_NAME = "manifest"
private const val KLIB_METADATA_FOLDER_NAME = "linkdata"
private const val KLIB_MODULE_METADATA_FILE_NAME = "module"
private const val KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME = "root_package"
private const val KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX = "package_"
private const val KLIB_METADATA_FILE_EXTENSION = "knm"
private const val KLIB_IR_FOLDER_NAME = "ir"

/**
 * Asserts that the compilation produced the skeleton of an unpacked metadata klib: the klib manifest
 * (`default/manifest`) and the module metadata header (`default/linkdata/module`). These are the stable markers that
 * the output is a valid klib directory rather than just some files, without pinning the package fragment names.
 */
context(module: ModuleContext)
fun CompilationOutcome.assertIsUnpackedMetadataKlib() {
    assertOutputsContains(
        "$KLIB_DEFAULT_COMPONENT_DIR/$KLIB_MANIFEST_FILE_NAME",
        "$KLIB_DEFAULT_COMPONENT_DIR/$KLIB_METADATA_FOLDER_NAME/$KLIB_MODULE_METADATA_FILE_NAME",
    )
}

/**
 * Asserts that the compilation produced the skeleton of an unpacked klib with IR: the klib manifest
 * (`default/manifest`), the module metadata header (`default/linkdata/module`) and a non-empty IR component
 * (`default/ir`).
 *
 * Unlike [assertIsUnpackedMetadataKlib], the IR component is required here: it is exactly what the linking operation
 * consumes to produce the final artifact. The individual IR file names are not pinned because they are an
 * implementation detail of the klib IR serializer.
 */
context(module: ModuleContext)
fun CompilationOutcome.assertIsUnpackedKlibWithIr() {
    assertOutputsContains(
        "$KLIB_DEFAULT_COMPONENT_DIR/$KLIB_MANIFEST_FILE_NAME",
        "$KLIB_DEFAULT_COMPONENT_DIR/$KLIB_METADATA_FOLDER_NAME/$KLIB_MODULE_METADATA_FILE_NAME",
    )
    val irDirectory = module.outputDirectory.resolve(KLIB_DEFAULT_COMPONENT_DIR).resolve(KLIB_IR_FOLDER_NAME)
    val irFiles = if (irDirectory.exists()) {
        irDirectory.walk().filter { it.isRegularFile() }.toList()
    } else {
        emptyList()
    }
    assert(irFiles.isNotEmpty()) {
        "Expected a non-empty klib IR component in $irDirectory, but found none.\nFiles in the output directory:\n${
            module.outputDirectory.walk().joinToString("\n")
        }"
    }
}

/**
 * Reads the manifest (`default/manifest`) of the module's unpacked klib output.
 */
context(module: ModuleContext)
fun readUnpackedKlibManifest(): Map<String, String> {
    val manifest = module.outputDirectory.resolve(KLIB_DEFAULT_COMPONENT_DIR).resolve(KLIB_MANIFEST_FILE_NAME)
    assert(manifest.exists()) {
        "The klib manifest $manifest does not exist.\nFiles in the output directory:\n${
            module.outputDirectory.walk().joinToString("\n")
        }"
    }
    val properties = Properties()
    manifest.inputStream().use(properties::load)
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}

/**
 * Asserts that the klib manifest of the module's unpacked klib output contains exactly the given
 * [expectedProperties] entries.
 */
context(module: ModuleContext)
fun assertKlibManifestProperties(vararg expectedProperties: Pair<String, String>) {
    val manifest = readUnpackedKlibManifest()
    val mismatched = expectedProperties.filterNot { manifest[it.first] == it.second }
    assert(mismatched.isEmpty()) {
        "The klib manifest does not contain the expected properties: ${
            mismatched.joinToString { "${it.first}=${it.second} (actual: ${manifest[it.first]})" }
        }.\n\nManifest contents:\n${manifest.entries.sortedBy { it.key }.joinToString("\n")}"
    }
}

/**
 * Asserts that the klib manifest of the module's unpacked klib output declares none of [propertyNames].
 */
context(module: ModuleContext)
fun assertKlibManifestHasNoProperties(vararg propertyNames: String) {
    val manifest = readUnpackedKlibManifest()
    val unexpected = propertyNames.filter { it in manifest }
    assert(unexpected.isEmpty()) {
        "The klib manifest declares the following properties, but was not expected to: ${
            unexpected.joinToString { "$it=${manifest[it]}" }
        }.\n\nManifest contents:\n${manifest.entries.sortedBy { it.key }.joinToString("\n")}"
    }
}

/**
 * Asserts that the compilation produced a packed klib file named `<moduleName>.klib` and no unpacked klib component
 * directory next to it.
 */
context(module: ModuleContext)
fun CompilationOutcome.assertIsPackedKlib(moduleName: String) {
    assertOutputsContains("$moduleName.klib")
    val componentDirectory = module.outputDirectory.resolve(KLIB_DEFAULT_COMPONENT_DIR)
    assert(!componentDirectory.exists()) {
        "Expected a packed klib file only, but the unpacked klib component directory $componentDirectory exists as well"
    }
}

/**
 * The directory (relative to the klib metadata `linkdata` folder) that stores the fragments of [packageFqName]: the
 * root package (empty string) lives in `root_package`, a named package `foo.bar` in `package_foo.bar`.
 */
private fun linkDataPackageFolderName(packageFqName: String): String =
    if (packageFqName.isEmpty()) {
        KLIB_ROOT_PACKAGE_FRAGMENT_FOLDER_NAME
    } else {
        "$KLIB_NONROOT_PACKAGE_FRAGMENT_FOLDER_PREFIX$packageFqName"
    }

/**
 * Asserts that the compilation produced exactly [expectedCount] `.knm` files (klib metadata package fragments) for the
 * package [packageFqName] in the module's unpacked metadata klib output. [packageFqName] defaults to the root package
 * (empty string), so it can be omitted when asserting the root package.
 */
context(module: ModuleContext)
fun assertKnmFileCount(expectedCount: Int, packageFqName: String = "") {
    val packageDirectory = module.outputDirectory
        .resolve(KLIB_DEFAULT_COMPONENT_DIR)
        .resolve(KLIB_METADATA_FOLDER_NAME)
        .resolve(linkDataPackageFolderName(packageFqName))
    val knmFiles = if (packageDirectory.exists()) {
        packageDirectory.walk()
            .filter { it.isRegularFile() && it.fileName.toString().endsWith(".$KLIB_METADATA_FILE_EXTENSION") }
            .map { it.relativeTo(module.outputDirectory).toString() }
            .toList()
    } else {
        emptyList()
    }
    assertEquals(expectedCount, knmFiles.size) {
        "Expected $expectedCount .$KLIB_METADATA_FILE_EXTENSION file(s) in $packageDirectory, but found ${knmFiles.size}: ${knmFiles.sorted()}"
    }
}

/**
 * Asserts that the compilation produced no `.knm` files (klib metadata package fragments) anywhere under the module's
 * output directory.
 *
 * Useful for the empty-source case: with no sources the compiler creates no package fragment directories
 * (`root_package`, `package_<fqName>`) at all, so there is no specific directory to point [assertKnmFileCount] at.
 */
context(module: ModuleContext)
fun assertNoKnmFiles() {
    val knmFiles = module.outputDirectory.walk()
        .filter { it.isRegularFile() && it.fileName.toString().endsWith(".$KLIB_METADATA_FILE_EXTENSION") }
        .map { it.relativeTo(module.outputDirectory).toString() }
        .toList()
    assertEquals(0, knmFiles.size) {
        "Expected no .$KLIB_METADATA_FILE_EXTENSION files, but found ${knmFiles.size}: ${knmFiles.sorted()}"
    }
}
