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
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult

internal fun validateExportedLibraries(configuration: CompilerConfiguration, resolvedLibraries: KonanLibraryResolveResult) {
    getExportedLibraries(configuration, resolvedLibraries, report = true)
}

internal fun Context.getExportedDependencies(): List<ModuleDescriptor> {
    val exportedLibraries = getExportedLibraries(this.config.configuration, this.config.resolvedLibraries, report = false)
            .toSet()

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

private fun getExportedLibraries(
        configuration: CompilerConfiguration,
        resolvedLibraries: KonanLibraryResolveResult,
        report: Boolean
): List<KonanLibrary> {
    val exportedLibraries = configuration.getList(KonanConfigKeys.EXPORTED_LIBRARIES).map { File(it) }.toSet()
    val remainingExportedLibraries = exportedLibraries.toMutableSet()

    val result = mutableListOf<KonanLibrary>()

    val libraries = resolvedLibraries.getFullList(null)

    for (library in libraries) {
        val libraryFile = library.libraryFile
        if (libraryFile in exportedLibraries) {
            remainingExportedLibraries -= libraryFile
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

    if (report && remainingExportedLibraries.isNotEmpty()) {
        val message = buildString {
            appendln("Following libraries are specified to be exported with -Xexport-library, but not included to the build:")
            remainingExportedLibraries.forEach { appendln(it) }
            appendln()
            appendln("Included libraries:")
            libraries.forEach { appendln(it.libraryFile) }
        }

        configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
    }

    return result
}