/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.CurrentKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.DeserializedKonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.descriptors.konan.konanModuleOrigin
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.SearchPathResolver
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.library.toUnresolvedLibraries

internal fun Context.getExportedDependencies(): List<ModuleDescriptor> {
    val exportedLibraries = this.config.exportedLibraries.toSet()

    val result = this.moduleDescriptor.allDependencyModules.filter {
        val origin = it.konanModuleOrigin
        when (origin) {
            CurrentKonanModuleOrigin, SyntheticModulesOrigin -> false
            is DeserializedKonanModuleOrigin -> {
                origin.library in exportedLibraries
            }
        }
    }

    return result
}

internal fun getExportedLibraries(
        configuration: CompilerConfiguration,
        resolvedLibraries: KonanLibraryResolveResult,
        resolver: SearchPathResolver,
        report: Boolean
): List<KonanLibrary> = getFeaturedLibraries(
    configuration.getList(KonanConfigKeys.EXPORTED_LIBRARIES),
    configuration,
    resolvedLibraries,
    resolver,
    report
)

private fun getFeaturedLibraries(
    featuredLibraries: List<String>,
    configuration: CompilerConfiguration,
    resolvedLibraries: KonanLibraryResolveResult,
    resolver: SearchPathResolver,
    report: Boolean = false
) : List<KonanLibrary> {
    val featuredLibraryFiles = featuredLibraries.toUnresolvedLibraries.map { resolver.resolve(it).libraryFile }.toSet()
    val remainingFeaturedLibraries = featuredLibraryFiles.toMutableSet()

    val result = mutableListOf<KonanLibrary>()

    val libraries = resolvedLibraries.getFullList(null)

    for (library in libraries) {
        val libraryFile = library.libraryFile
        if (libraryFile in featuredLibraryFiles) {
            remainingFeaturedLibraries -= libraryFile
            if (library.isInterop || library.isDefault) {
                if (report) {
                    val kind = if (library.isInterop) "Interop" else "Default"
                    configuration.report(
                        CompilerMessageSeverity.STRONG_WARNING,
                        "$kind library ${library.libraryName} can't be exported with -Xexport-library"
                    )
                }
            } else {
                result += library
            }
        }
    }

    if (report && remainingFeaturedLibraries.isNotEmpty()) {
        val message = buildString {
            appendln("Following libraries are specified to be exported with -Xexport-library, but not included to the build:")
            remainingFeaturedLibraries.forEach { appendln(it) }
            appendln()
            appendln("Included libraries:")
            libraries.forEach { appendln(it.libraryFile) }
        }

        configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
    }

    return result
}