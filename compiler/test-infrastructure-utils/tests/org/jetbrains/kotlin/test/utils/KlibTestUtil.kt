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

inline fun patchManifest(
    assertions: Assertions,
    klibDir: File,
    transform: (String, String) -> String
) {
    assertions.assertTrue(klibDir.exists()) { "KLIB directory does not exist: ${klibDir.absolutePath}" }
    assertions.assertTrue(klibDir.isDirectory) { "Unpacked KLIB expected: ${klibDir.absolutePath}" }

    val manifestFile = klibDir.resolve("default/manifest")
    assertions.assertTrue(manifestFile.isFile) { "No manifest file: $manifestFile" }

    Properties().apply {
        manifestFile.inputStream().use { load(it) }
        entries.forEach { entry -> entry.setValue(transform(entry.key.toString(), entry.value.toString())) }
        manifestFile.outputStream().use { store(it, null) }
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

fun assertCompilerOutputHasKlibResolverIncompatibleAbiMessages(
    assertions: Assertions,
    compilerOutput: String,
    missingLibrary: String,
    baseDir: File
) {
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

    assertHasLineWithPrefix("error: KLIB resolver: Could not find \"<path>$missingLibraryPath\"")
    assertHasLineWithPrefix("warning: KLIB resolver: Skipping '<path>$missingLibraryPath'. Incompatible ABI version")
}
