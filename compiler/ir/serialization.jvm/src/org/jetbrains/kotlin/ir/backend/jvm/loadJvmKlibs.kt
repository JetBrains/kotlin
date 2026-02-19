package org.jetbrains.kotlin.ir.backend.jvm

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.klibPaths
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.library.loader.KlibLoader

/**
 * This is the entry point to load Kotlin/JVM experimental KLIBs.
 *
 * @param configuration The current compiler configuration.
 */
fun loadJvmKlibs(configuration: CompilerConfiguration): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(configuration.klibPaths)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it)}
    }.load()
    result.reportLoadingProblemsIfAny(configuration, allAsErrors = true)
    return LoadedKlibs(all = result.librariesStdlibFirst)
}
