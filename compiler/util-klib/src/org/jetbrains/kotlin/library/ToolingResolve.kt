/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
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
    override fun resolve(libraryFile: File, logger: Logger): KotlinLibrary {
        if (checkComponent(libraryFile)) {
            // old style library
            return fakeLibrary(libraryFile)
        }

        val componentFiles = libraryFile.listFiles.filter(::checkComponent)
        componentFiles.singleOrNull()?.let { componentFile ->
            // single component library
            return createKotlinLibrary(libraryFile, componentFile.name)
        }

        // otherwise mimic as old style library and warn
        if (componentFiles.isNotEmpty()) {
            // TODO: choose the best fit among all available candidates

            logger.warning("Library $libraryFile can not be read. Multiple components found: ${componentFiles.map {
                it.path.substringAfter(libraryFile.path)
            }}")
        }

        return fakeLibrary(libraryFile)
    }

    private const val NONEXISTENT_COMPONENT_NAME = "__nonexistent_component_name__"

    private fun checkComponent(componentFile: File) = componentFile.child(KLIB_MANIFEST_FILE_NAME).isFile
    private fun fakeLibrary(libraryFile: File) = createKotlinLibrary(libraryFile, NONEXISTENT_COMPONENT_NAME)
}
