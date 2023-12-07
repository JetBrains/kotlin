/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.util.Logger
import java.io.IOException

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

    fun tryResolve(libraryFile: File, logger: Logger): KotlinLibrary? =
        withSafeAccess(libraryFile) { localRoot ->
            if (localRoot.looksLikeKlibComponent) {
                // old style library
                null
            } else {
                val components = localRoot.listFiles.filter { it.looksLikeKlibComponent }
                when (components.size) {
                    0 -> null
                    1 -> {
                        // single component library
                        createKotlinLibrary(libraryFile, components.single().name)
                    }
                    else -> { // TODO: choose the best fit among all available candidates
                        // mimic as old style library and warn
                        logger.warning(
                            "KLIB resolver: Library '$libraryFile' can not be read." +
                                    " Multiple components found: ${components.map { it.path.substringAfter(localRoot.path) }}"
                        )

                        null
                    }
                }
            }
        }

    private const val NONEXISTENT_COMPONENT_NAME = "__nonexistent_component_name__"

    private fun fakeLibrary(libraryFile: File): KotlinLibrary = createKotlinLibrary(libraryFile, NONEXISTENT_COMPONENT_NAME)

    private fun <T : Any> withSafeAccess(libraryFile: File, action: (localRoot: File) -> T?): T? {
        val extension = libraryFile.extension

        val wrappedAction: () -> T? = when {
            libraryFile.isDirectory -> {
                { action(libraryFile) }
            }
            libraryFile.isFile && extension == KLIB_FILE_EXTENSION -> {
                { libraryFile.withZipFileSystem { fs -> action(fs.file("/")) } }
            }
            else -> return null
        }

        return try {
            wrappedAction()
        } catch (_: IOException) {
            null
        }
    }

    private val File.looksLikeKlibComponent: Boolean
        get() = child(KLIB_MANIFEST_FILE_NAME).isFile
}
