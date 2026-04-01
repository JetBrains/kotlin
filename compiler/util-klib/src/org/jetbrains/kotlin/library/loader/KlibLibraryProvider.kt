/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.library.Klib

/**
 * A component that provides a list of libraries to be loaded by [KlibLoader]. Allows post-processing each loaded library
 * by calling [KlibLibraryProvider.postProcessLoadedLibrary].
 *
 * This is actually an extended version of [KlibLoaderSpec.libraryPaths], that itself is built on top of [DefaultKlibLibraryProvider].
 */
interface KlibLibraryProvider {
    /**
     * Returns the ordered list of library paths to be loaded.
     */
    fun getLibraryPaths(): List<String>

    /**
     * Called for each library after it has been successfully loaded.
     */
    fun postProcessLoadedLibrary(klib: Klib)
}

class DefaultKlibLibraryProvider(libraryPaths: List<String>) : KlibLibraryProvider {
    private val libraryPaths = libraryPaths.toList()

    override fun getLibraryPaths() = libraryPaths
    override fun postProcessLoadedLibrary(klib: Klib) = Unit
}
