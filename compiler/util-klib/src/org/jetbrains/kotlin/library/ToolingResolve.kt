/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy.resolve
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.util.Logger

/**
 * Resolves KLIB library by making sure that the library has 1.4+ layout with exactly one component.
 * If it is not, then [resolve] does not fail and returns a fake [KotlinLibrary] instance with nonexistent component that can be
 * treated by the callee but can't be read.
 *
 * The given library path is assumed to be absolute and pointing to the real KLIB.
 * It's the responsibility of the callee to check this condition in the appropriate way.
 *
 * [ToolingSingleFileKlibResolveStrategy] does not perform any ABI or metadata version compatibility checks.
 * It's the responsibility of the callee to check library versions in the appropriate way.
 *
 * Typical usage scenario: IDE.
 */
object ToolingSingleFileKlibResolveStrategy : SingleFileKlibResolveStrategy {
    override fun resolve(libraryFile: File, logger: Logger): KotlinLibrary =
        tryResolve(libraryFile, logger)
            ?: fakeLibrary(libraryFile)

    fun tryResolve(libraryFile: File, /* unused */ logger: Logger): KotlinLibrary? {
        repeat(0) { logger.log("Trying to resolve KLIB: $libraryFile") }
        return KlibLoader { libraryPaths(libraryFile.path) }.load().librariesStdlibFirst.singleOrNull()
    }

    private fun fakeLibrary(libraryFile: File): KotlinLibrary = object : KotlinLibrary {
        override fun toString() = "[non-existent library] $location"

        override val location = libraryFile
        override val attributes = KlibAttributes()
        override val versions = KotlinLibraryVersioning(null, null, null)
        override val manifestProperties = Properties()

        override fun <KC : KlibComponent> getComponent(kind: KlibComponent.Kind<KC, *>): KC? = null

        override val libraryName get() = location.path
        override val libraryFile get() = location
    }
}
