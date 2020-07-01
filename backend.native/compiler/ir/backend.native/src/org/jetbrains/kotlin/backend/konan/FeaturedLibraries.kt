/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.library.isInterop
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal fun Context.getExportedDependencies(): List<ModuleDescriptor> = getDescriptorsFromLibraries((config.resolve.exportedLibraries + config.resolve.includedLibraries).toSet())
internal fun Context.getIncludedLibraryDescriptors(): List<ModuleDescriptor> = getDescriptorsFromLibraries(config.resolve.includedLibraries.toSet())

private fun Context.getDescriptorsFromLibraries(libraries: Set<KonanLibrary>) =
    moduleDescriptor.allDependencyModules.filter {
        when (val origin = it.klibModuleOrigin) {
            CurrentKlibModuleOrigin, SyntheticModulesOrigin -> false
            is DeserializedKlibModuleOrigin -> origin.library in libraries
        }
    }

internal fun getExportedLibraries(
    configuration: CompilerConfiguration,
    resolvedLibraries: KotlinLibraryResolveResult,
    resolver: SearchPathResolver<KonanLibrary>,
    report: Boolean
): List<KonanLibrary> = getFeaturedLibraries(
        configuration.getList(KonanConfigKeys.EXPORTED_LIBRARIES),
        resolvedLibraries,
        resolver,
        if (report) FeaturedLibrariesReporter.forExportedLibraries(configuration) else FeaturedLibrariesReporter.Silent,
        allowDefaultLibs = false
)

internal fun getIncludedLibraries(
    includedLibraryFiles: List<File>,
    configuration: CompilerConfiguration,
    resolvedLibraries: KotlinLibraryResolveResult
): List<KonanLibrary> = getFeaturedLibraries(
        includedLibraryFiles.toSet(),
        resolvedLibraries,
        FeaturedLibrariesReporter.forIncludedLibraries(configuration),
        allowDefaultLibs = false
)

internal fun getCoveredLibraries(
    configuration: CompilerConfiguration,
    resolvedLibraries: KotlinLibraryResolveResult,
    resolver: SearchPathResolver<KonanLibrary>
): List<KonanLibrary> = getFeaturedLibraries(
        configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER),
        resolvedLibraries,
        resolver,
        FeaturedLibrariesReporter.forCoveredLibraries(configuration),
        allowDefaultLibs = true
)

private sealed class FeaturedLibrariesReporter {

    abstract fun reportIllegalKind(library: KonanLibrary)
    abstract fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>)

    protected val KonanLibrary.reportedKind: String
        get() = when {
            isInterop -> "Interop"
            isDefault -> "Default"
            else -> "Unknown kind"
        }

    object Silent: FeaturedLibrariesReporter() {
        override fun reportIllegalKind(library: KonanLibrary) {}
        override fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>) {}
    }

    abstract class BaseReporter(val configuration: CompilerConfiguration) : FeaturedLibrariesReporter() {
        protected abstract fun illegalKindMessage(kind: String, libraryName: String): String
        protected abstract fun notIncludedLibraryMessageTitle(): String

        override fun reportIllegalKind(library: KonanLibrary) {
            configuration.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    illegalKindMessage(library.reportedKind, library.libraryName)
            )
        }

        override fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>) {
            val message = buildString {
                appendLine(notIncludedLibraryMessageTitle())
                remainingFeaturedLibraries.forEach { appendLine(it) }
                appendLine()
                appendLine("Included libraries:")
                includedLibraries.forEach { appendLine(it.libraryFile) }
            }

            configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }
    }

    private class IncludedLibrariesReporter(val configuration: CompilerConfiguration) : FeaturedLibrariesReporter() {
        override fun reportIllegalKind(library: KonanLibrary) = with(library) {
            val message = "$reportedKind library $libraryName cannot be passed with -Xinclude " +
                    "(library path: ${libraryFile.absolutePath})"
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }

        override fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>) {
            error("An included library is not found among resolved libraries")
        }
    }

    private class ExportedLibrariesReporter(configuration: CompilerConfiguration) : BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "$kind library $libraryName can't be exported with -Xexport-library"

        override fun notIncludedLibraryMessageTitle(): String =
            "Following libraries are specified to be exported with -Xexport-library, but not included to the build:"
    }

    private class CoveredLibraryReporter(configuration: CompilerConfiguration): BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "Cannot provide the code coverage for the $kind library $libraryName."

        override fun notIncludedLibraryMessageTitle(): String =
            "The code coverage is enabled for the following libraries, but they are not included to the build:"
    }

    companion object {
        fun forExportedLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter =
                ExportedLibrariesReporter(configuration)
        fun forCoveredLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter =
                CoveredLibraryReporter(configuration)
        fun forIncludedLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter =
                IncludedLibrariesReporter(configuration)
    }
}

private fun getFeaturedLibraries(
        featuredLibraries: List<String>,
        resolvedLibraries: KotlinLibraryResolveResult,
        resolver: SearchPathResolver<KonanLibrary>,
        reporter: FeaturedLibrariesReporter,
        allowDefaultLibs: Boolean
) = getFeaturedLibraries(
        featuredLibraries.toUnresolvedLibraries.map { resolver.resolve(it).libraryFile }.toSet(),
        resolvedLibraries,
        reporter,
        allowDefaultLibs
)

private fun getFeaturedLibraries(
    featuredLibraryFiles: Set<File>,
    resolvedLibraries: KotlinLibraryResolveResult,
    reporter: FeaturedLibrariesReporter,
    allowDefaultLibs: Boolean
) : List<KonanLibrary> {
    val remainingFeaturedLibraries = featuredLibraryFiles.toMutableSet()
    val result = mutableListOf<KonanLibrary>()
    //TODO: please add type checks before cast.
    val libraries = resolvedLibraries.getFullList(null).cast<List<KonanLibrary>>()

    for (library in libraries) {
        val libraryFile = library.libraryFile
        if (libraryFile in featuredLibraryFiles) {
            remainingFeaturedLibraries -= libraryFile
            if (library.isInterop || (!allowDefaultLibs && library.isDefault)) {
                reporter.reportIllegalKind(library)
            } else {
                result += library
            }
        }
    }

    if (remainingFeaturedLibraries.isNotEmpty()) {
        reporter.reportNotIncludedLibraries(libraries, remainingFeaturedLibraries)
    }

    return result
}