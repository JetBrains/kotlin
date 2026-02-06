/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.resolve

import org.jetbrains.kotlin.backend.konan.serialization.KonanLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.klibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.skipCompatibilityChecks
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.config.konanIncludedLibraries
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanLibraryToAddToCache
import org.jetbrains.kotlin.konan.config.konanNoDefaultLibs
import org.jetbrains.kotlin.konan.config.konanNoEndorsedLibs
import org.jetbrains.kotlin.konan.config.konanNoStdlib
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.library.validateNoLibrariesWerePassedViaCliByUniqueName

class KonanLibrariesResolveSupport(
    configuration: CompilerConfiguration,
    target: KonanTarget,
    distribution: Distribution,
    resolveManifestDependenciesLenient: Boolean
) {
    private val includedLibraryFiles =
            configuration.konanIncludedLibraries.map { File(it) }

    private val libraryToCacheFile =
                    configuration.konanLibraryToAddToCache?.let { File(it) }

    private val libraryPaths = configuration.konanLibraries

    private val unresolvedLibraries = libraryPaths.toUnresolvedLibraries

    val resolver = defaultResolver(
        libraryPaths + includedLibraryFiles.map { it.absolutePath },
        target,
        distribution,
        configuration.getLogger(),
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
            unresolvedLibraries + additionalLibraryFiles.map { RequiredUnresolvedLibrary(it.absolutePath) },
            noStdLib = configuration.konanNoStdlib,
            noDefaultLibs = configuration.konanNoDefaultLibs,
            noEndorsedLibs = configuration.konanNoEndorsedLibs,
            duplicatedUniqueNameStrategy = configuration.get(
                KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                DuplicatedUniqueNameStrategy.DENY
            ),
        ).also { resolvedLibraries ->
            validateNoLibrariesWerePassedViaCliByUniqueName(libraryPaths, resolvedLibraries.getFullList(), resolver.logger)
            if (!configuration.skipCompatibilityChecks) {
                KonanLibrarySpecialCompatibilityChecker.check(
                    resolvedLibraries.getFullList(),
                    configuration.messageCollector,
                    configuration.klibAbiCompatibilityLevel
                )
            }
        }
    }
}