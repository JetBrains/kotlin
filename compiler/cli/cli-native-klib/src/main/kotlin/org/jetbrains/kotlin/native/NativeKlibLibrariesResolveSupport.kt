/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.cli.common.messages.getLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.library.validateNoLibrariesWerePassedViaCliByUniqueName
import org.jetbrains.kotlin.konan.file.File

/**
 * Library resolution support for Native klib compilation.
 *
 * This class provides library resolution functionality similar to KonanLibrariesResolveSupport
 * from the full Native backend, but without dependencies on the backend infrastructure.
 */
class NativeKlibLibrariesResolveSupport(
    configuration: CompilerConfiguration,
    target: KonanTarget,
    distribution: Distribution,
    resolveManifestDependenciesLenient: Boolean = true,
) {
    private val includedLibraryFiles =
        configuration.getList(KonanConfigKeys.INCLUDED_LIBRARIES).map { File(it) }

    private val libraryPaths = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val unresolvedLibraries = libraryPaths.toUnresolvedLibraries

    private val resolver = defaultResolver(
        libraryPaths + includedLibraryFiles.map { it.absolutePath },
        target,
        distribution,
        configuration.getLogger(),
        false,
        configuration.zipFileSystemAccessor
    ).libraryResolver(resolveManifestDependenciesLenient)

    val resolvedLibraries: KotlinLibraryResolveResult = run {
        val additionalLibraryFiles = includedLibraryFiles.toSet()
        resolver.resolveWithDependencies(
            unresolvedLibraries + additionalLibraryFiles.map { RequiredUnresolvedLibrary(it.absolutePath) },
            noStdLib = configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
            noDefaultLibs = configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS),
            noEndorsedLibs = configuration.getBoolean(KonanConfigKeys.NOENDORSEDLIBS),
            duplicatedUniqueNameStrategy = configuration.get(
                KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                DuplicatedUniqueNameStrategy.DENY
            ),
        ).also { resolvedLibraries ->
            validateNoLibrariesWerePassedViaCliByUniqueName(libraryPaths, resolvedLibraries.getFullList(), resolver.logger)
        }
    }
}
