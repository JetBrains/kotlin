/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.resolve

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.library.validateNoLibrariesWerePassedViaCliByUniqueName
import org.jetbrains.kotlin.util.Logger
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

class KonanLibrariesResolveSupport(
    configuration: CompilerConfiguration,
    target: KonanTarget,
    distribution: Distribution,
    resolveManifestDependenciesLenient: Boolean
) {
    private val includedLibraryFiles: List<Path> = configuration.konanIncludedLibraries.map { Path(it) }

    private val libraryToCacheFile: Path? = configuration.konanLibraryToAddToCache?.let { Path(it) }

    private val libraryPaths = configuration.konanLibraries

    private val unresolvedLibraries = libraryPaths.toUnresolvedLibraries

    val resolver = defaultResolver(
        libraryPaths + includedLibraryFiles.map { it.absolutePathString() },
        target,
        distribution,
        CompilerLoggerAdapter(configuration),
        false,
        configuration.zipFileSystemAccessor
    ).libraryResolver(resolveManifestDependenciesLenient)

    // We pass included libraries by absolute paths to avoid repository-based resolution for them.
    // Strictly speaking such "direct" libraries should be specially handled by the resolver, not by KonanConfig.
    // But currently the resolver is in the middle of a complex refactoring so it was decided to avoid changes in its logic.
    // TODO: Handle included libraries in KonanLibraryResolver when it's refactored and moved into the big Kotlin repo.
    val resolvedLibraries = run {
        val additionalLibraryFiles = (includedLibraryFiles + listOfNotNull(libraryToCacheFile)).toSet()
        resolver.resolveWithDependencies(
            unresolvedLibraries + additionalLibraryFiles.map { RequiredUnresolvedLibrary(it.absolutePathString()) },
            noStdLib = configuration.konanNoStdlib,
            noDefaultLibs = configuration.konanNoDefaultLibs,
            duplicatedUniqueNameStrategy = configuration.get(
                KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                DuplicatedUniqueNameStrategy.DENY
            ),
        ).also { resolvedLibraries ->
            validateNoLibrariesWerePassedViaCliByUniqueName(libraryPaths, resolvedLibraries.getFullList(), resolver.logger)
        }
    }
}

private class CompilerLoggerAdapter(
    private val configuration: CompilerConfiguration
) : Logger {
    override fun log(message: String) = configuration.reportLog(message)
    override fun warning(message: String) = configuration.report(CliDiagnostics.KONAN_ARGUMENT_WARNING, message)
    override fun strongWarning(message: String) = configuration.report(CliDiagnostics.KONAN_ARGUMENT_STRONG_WARNING, message)
    override fun error(message: String) = configuration.report(CliDiagnostics.KONAN_ARGUMENT_ERROR, message)

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String): Nothing {
        configuration.report(CliDiagnostics.KONAN_COMPILATION_ERROR, message)
        @OptIn(MessageCollectorAccess::class)
        (configuration.messageCollector as? GroupingMessageCollector)?.flush()
        throw CompilationErrorException(message)
    }
}
