/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.library.loader.KlibLoader

/**
 * This is the entry point to load metadata-only KLIBs.
 *
 * @param configuration The current compiler configuration.
 * @param libraryPaths Paths of libraries to load.
 */
fun loadMetadataKlibs(configuration: CompilerConfiguration, libraryPaths: List<String>): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(libraryPaths)
        // IMPORTANT: Do not set any ABI version requirements - metadata libraries are not supposed to have any ABI.
    }.load()
    result.reportLoadingProblemsIfAny(configuration.messageCollector)
    return LoadedKlibs(all = result.librariesStdlibFirst)
}
