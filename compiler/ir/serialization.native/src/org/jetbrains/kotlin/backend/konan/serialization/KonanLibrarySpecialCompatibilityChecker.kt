/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib

object KonanLibrarySpecialCompatibilityChecker : LibrarySpecialCompatibilityChecker() {
    override fun KotlinLibrary.toCheckedLibrary(): CheckedLibrary? = when {
        isNativeStdlib -> CheckedLibrary(libraryDisplayName = "standard", platformDisplayName = "Kotlin/Native")
        else -> null
    }

    /**
     * Unlike other Kotlin libraries, the K/N stdlib does not store its version in `META-INF/MANIFEST.MF`
     * because it isn’t published as a standalone artifact.
     *
     * To retrieve the stdlib version (which may differ from the compiler version when exporting a klib to an older ABI version),
     * we assume the stdlib resides within a K/N distribution and read the version
     * from the distribution's `konan.properties` file.
     */
    override fun libraryVersion(library: KotlinLibrary): Version? =
        super.libraryVersion(library) ?: Version.parseVersion(getCompilerVersionFromKonanProperties(library))

    private fun getCompilerVersionFromKonanProperties(library: KotlinLibrary): String? {
        if (!library.libraryFile.path.endsWith(KONAN_STDLIB_DIRECTORY)) return null

        val konanRoot = library.libraryFile.path.substringBefore(KONAN_STDLIB_DIRECTORY)
        val propertiesPath = konanRoot + KONAN_PROPERTIES_FILE
        val propertiesFile = File(propertiesPath)
        if (!propertiesFile.exists) return null

        val properties = propertiesFile.loadProperties()
        return properties.getProperty(KONAN_COMPILER_VERSION)
    }

    private const val KONAN_PROPERTIES_FILE = "konan/konan.properties"
    private const val KONAN_STDLIB_DIRECTORY = "klib/common/stdlib"
    private const val KONAN_COMPILER_VERSION = "compilerVersion"
}