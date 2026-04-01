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
