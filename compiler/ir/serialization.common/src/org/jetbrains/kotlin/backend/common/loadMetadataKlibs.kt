/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.library.loader.KlibLoader

/**
 * This is the entry point to load metadata-only KLIBs.
 *
 * @param libraryPaths Paths of libraries to load.
 * @param configuration The current compiler configuration.
 */
fun loadMetadataKlibs(libraryPaths: List<String>, configuration: CompilerConfiguration): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(libraryPaths)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it)}
        // IMPORTANT: Do not set any ABI version requirements - metadata libraries are not supposed to have any ABI.
    }.load()
    result.reportLoadingProblemsIfAny(configuration)
    return LoadedKlibs(all = result.librariesStdlibFirst)
}
