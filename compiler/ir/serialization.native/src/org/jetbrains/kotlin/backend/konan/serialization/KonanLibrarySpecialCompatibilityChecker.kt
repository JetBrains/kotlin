/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.diagnostics.LibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isNativeStdlib
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    fun getCompilerVersionFromKonanProperties(library: KotlinLibrary): String? {
        val libraryPath = Paths.get(library.libraryFile.path)

        if (!libraryPath.endsWith(KONAN_STDLIB_DIRECTORY)) return null

        val konanRoot = libraryPath.root?.resolve(
            libraryPath.subpath(0, libraryPath.nameCount - KONAN_STDLIB_DIRECTORY.nameCount)
        ) ?: return null

        val propertiesPath = konanRoot.resolve(KONAN_PROPERTIES_FILE)
        if (!Files.exists(propertiesPath)) return null

        val properties = loadProperties(propertiesPath.toString())
        return properties.getProperty(KONAN_COMPILER_VERSION)
    }

    private val KONAN_PROPERTIES_FILE: Path = Paths.get("konan", "konan.properties")
    private val KONAN_STDLIB_DIRECTORY: Path = Paths.get("klib", "common", "stdlib")
    private const val KONAN_COMPILER_VERSION = "compilerVersion"
}