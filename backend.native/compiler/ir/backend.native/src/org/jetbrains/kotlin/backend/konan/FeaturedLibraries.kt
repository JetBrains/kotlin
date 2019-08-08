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
import org.jetbrains.kotlin.konan.library.SearchPathResolver
import org.jetbrains.kotlin.konan.library.isInterop
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.library.toUnresolvedLibraries

internal fun Context.getExportedDependencies(): List<ModuleDescriptor> = getDescriptorsFromFeaturedLibraries((config.exportedLibraries + config.sourceLibraries).toSet())
internal fun Context.getSourceLibraryDescriptors(): List<ModuleDescriptor> = getDescriptorsFromFeaturedLibraries(config.sourceLibraries.toSet())

private fun Context.getDescriptorsFromFeaturedLibraries(featuredLibraries: Set<KonanLibrary>) =
    moduleDescriptor.allDependencyModules.filter {
        when (val origin = it.konanModuleOrigin) {
            CurrentKonanModuleOrigin, SyntheticModulesOrigin -> false
            is DeserializedKonanModuleOrigin -> origin.library in featuredLibraries
        }
    }

internal fun getExportedLibraries(
        configuration: CompilerConfiguration,
        resolvedLibraries: KonanLibraryResolveResult,
        resolver: SearchPathResolver,
        report: Boolean
): List<KonanLibrary> = getFeaturedLibraries(
    configuration.getList(KonanConfigKeys.EXPORTED_LIBRARIES),
    resolvedLibraries,
    resolver,
    if (report) FeaturedLibrariesReporter.forExportedLibraries(configuration) else FeaturedLibrariesReporter.Silent
)

internal fun getSourceLibraries(
    configuration: CompilerConfiguration,
    resolvedLibraries: KonanLibraryResolveResult,
    resolver: SearchPathResolver
): List<KonanLibrary> = getFeaturedLibraries(
    configuration.getList(KonanConfigKeys.SOURCE_LIBRARIES),
    resolvedLibraries,
    resolver,
    FeaturedLibrariesReporter.forSourceLibraries(configuration)
)

internal fun getCoveredLibraries(
    configuration: CompilerConfiguration,
    resolvedLibraries: KonanLibraryResolveResult,
    resolver: SearchPathResolver
): List<KonanLibrary> = getFeaturedLibraries(
    configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER),
    resolvedLibraries,
    resolver,
    FeaturedLibrariesReporter.forCoveredLibraries(configuration)
)

private sealed class FeaturedLibrariesReporter {

    abstract fun reportIllegalKind(library: KonanLibrary)
    abstract fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>)

    object Silent: FeaturedLibrariesReporter() {
        override fun reportIllegalKind(library: KonanLibrary) {}
        override fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>) {}

    }

    abstract class BaseReporter(val configuration: CompilerConfiguration) : FeaturedLibrariesReporter() {
        protected abstract fun illegalKindMessage(kind: String, libraryName: String): String
        protected abstract fun notIncludedLibraryMessageTitle(): String

        override fun reportIllegalKind(library: KonanLibrary) {
            val kind = if (library.isInterop) "Interop" else "Default"
            configuration.report(CompilerMessageSeverity.STRONG_WARNING, illegalKindMessage(kind, library.libraryName))
        }

        override fun reportNotIncludedLibraries(includedLibraries: List<KonanLibrary>, remainingFeaturedLibraries: Set<File>) {
            val message = buildString {
                appendln(notIncludedLibraryMessageTitle())
                remainingFeaturedLibraries.forEach { appendln(it) }
                appendln()
                appendln("Included libraries:")
                includedLibraries.forEach { appendln(it.libraryFile) }
            }

            configuration.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }
    }

    private class ExportedLibrariesReporter(configuration: CompilerConfiguration) : BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "$kind library $libraryName can't be exported with -Xexport-library"

        override fun notIncludedLibraryMessageTitle(): String =
            "Following libraries are specified to be exported with -Xexport-library, but not included to the build:"
    }

    private class SourceLibrariesReporter(configuration: CompilerConfiguration) : BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "$kind library $libraryName can't be used as a source library"

        override fun notIncludedLibraryMessageTitle(): String =
            "Following libraries are declared as source libraries with -Xsource-library, but not included to the build:"
    }

    private class CoveredLibraryReporter(configuration: CompilerConfiguration): BaseReporter(configuration) {
        override fun illegalKindMessage(kind: String, libraryName: String): String =
            "$kind library $libraryName can't be covered"

        override fun notIncludedLibraryMessageTitle(): String =
            "Following libraries are specified to be covered with -Xlibrary-to-cover, but not included to the build:"
    }

    companion object {
        fun forExportedLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter = ExportedLibrariesReporter(configuration)
        fun forSourceLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter = SourceLibrariesReporter(configuration)
        fun forCoveredLibraries(configuration: CompilerConfiguration): FeaturedLibrariesReporter = CoveredLibraryReporter(configuration)
    }
}

private fun getFeaturedLibraries(
    featuredLibraries: List<String>,
    resolvedLibraries: KonanLibraryResolveResult,
    resolver: SearchPathResolver,
    reporter: FeaturedLibrariesReporter
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