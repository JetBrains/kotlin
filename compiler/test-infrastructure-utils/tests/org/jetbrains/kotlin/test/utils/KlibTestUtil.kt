/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.test.Assertions
import java.io.File
import java.util.*

inline fun patchManifestAsMap(
    assertions: Assertions,
    klibDir: File,
    transform: (MutableMap<String, String>) -> Unit
) {
    assertions.assertTrue(klibDir.exists()) { "KLIB directory does not exist: ${klibDir.absolutePath}" }
    assertions.assertTrue(klibDir.isDirectory) { "Unpacked KLIB expected: ${klibDir.absolutePath}" }

    val manifestFile = klibDir.resolve("default/manifest")
    assertions.assertTrue(manifestFile.isFile) { "No manifest file: $manifestFile" }

    val mutableProperties: MutableMap<String, String> = Properties().apply {
        manifestFile.inputStream().use { load(it) }
    }.entries.associateTo(linkedMapOf()) { it.key.toString() to it.value.toString() }

    transform(mutableProperties)

    Properties().apply {
        for ((key, value) in mutableProperties) {
            this[key] = value
        }
        manifestFile.outputStream().use { store(it, null) }
    }
}

inline fun patchManifest(
    assertions: Assertions,
    klibDir: File,
    transform: (String, String) -> String
) {
    patchManifestAsMap(assertions, klibDir) { mutableProperties ->
        mutableProperties.entries.forEach { entry -> entry.setValue(transform(entry.key, entry.value)) }
    }
}

fun patchManifestToBumpAbiVersion(
    assertions: Assertions,
    klibDir: File,
    newAbiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT.copy(major = KotlinAbiVersion.CURRENT.major + 1)
) {
    patchManifest(assertions, klibDir) { key, value ->
        if (key == KLIB_PROPERTY_ABI_VERSION) newAbiVersion.toString() else value
    }
}

fun assertCompilerOutputHasKlibResolverIssue(
    assertions: Assertions,
    compilerOutput: String,
    missingLibrary: String,
    baseDir: File,
    prefixPatterns: List<(missingLibraryPath: String) -> String>
) {
    assertions.assertTrue(prefixPatterns.isNotEmpty())

    val baseDirPath = baseDir.absolutePath
    val missingLibraryPath = missingLibrary.replace('/', File.separatorChar).replace('\\', File.separatorChar)

    val lines = compilerOutput.lineSequence()
        .filter(String::isNotBlank)
        .map { it.replace(baseDirPath, "<path>") }
        .toList()

    fun assertHasLineWithPrefix(prefix: String) {
        if (lines.none { it.startsWith(prefix) }) {
            assertions.fail {
                buildString {
                    appendLine("No line starting with prefix found: $prefix")
                    appendLine("Lines inspected (${lines.size}):")
                    lines.forEach(::appendLine)
                }
            }
        }
    }

    prefixPatterns.forEach { prefixPattern ->
        val prefix = prefixPattern(missingLibraryPath)
        assertHasLineWithPrefix(prefix)
    }
}

fun assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(
    assertions: Assertions,
    compilerOutput: String,
    missingLibrary: String,
    baseDir: File
) {
    assertCompilerOutputHasKlibResolverIssue(
        assertions, compilerOutput, missingLibrary, baseDir,
        listOf(
            { "error: KLIB resolver: Could not find \"<path>$it\"" },
            { "warning: KLIB resolver: Skipping '<path>$it'. Incompatible ABI version" },
        )
    )
}
