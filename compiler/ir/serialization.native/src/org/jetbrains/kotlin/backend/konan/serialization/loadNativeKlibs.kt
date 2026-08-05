/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationErrors
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.loadFriendLibraries
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.library.KlibNativeManifestTransformer
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.irProviderName
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.OtherCheckMismatch
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblematicLibrary
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File
import kotlin.io.path.pathString

/**
 * This is the entry point to load Kotlin/Native KLIBs.
 * All library paths are read from the corresponding parameters of [CompilerConfiguration].
 *
 * @param configuration The current compiler configuration.
 * @param nativeTarget The Kotlin/Native-specific target.
 */
fun loadNativeKlibs(
    configuration: CompilerConfiguration,
    nativeTarget: KonanTarget,
): LoadedNativeKlibs {
    val loadStdlib = !configuration.konanNoStdlib
    val loadPlatformLibs = !configuration.konanNoDefaultLibs

    val nativeHome = configuration.konanHome?.let(::File) ?: KotlinNativePaths.homePath
    val distributionLibrariesProvider = KlibNativeDistributionLibraryProvider(nativeHome) {
        runIf(loadStdlib) { withStdlib() }
        runIf(loadPlatformLibs) { withPlatformLibs(nativeTarget) }
    }

    val platformChecker = if (configuration.metadataKlib)
        KlibPlatformChecker.NativeMetadata(nativeTarget.name)
    else
        KlibPlatformChecker.Native(nativeTarget.name)

    val result = KlibLoader {
        libraryProviders(distributionLibrariesProvider)
        libraryPaths(configuration.konanLibraries)
        platformChecker(platformChecker)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it) }
        manifestTransformer(KlibNativeManifestTransformer(nativeTarget))
    }.load()
        .checkForUnknownIrProviders()
        .apply { reportLoadingProblemsIfAny(configuration) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    if (!configuration.skipLibrarySpecialCompatibilityChecks) {
        KonanLibrarySpecialCompatibilityChecker.check(
            result.librariesStdlibFirst, configuration
        )
    }

    return LoadedNativeKlibs(
        all = result.librariesStdlibFirst,
        friends = result.loadFriendLibraries(configuration.konanFriendLibraries),
        included = result.loadFriendLibraries(configuration.konanIncludedLibraries)
            .selectLibrariesEligibleToBeIncluded(configuration)
    )
}

/**
 * Check whether all the loaded libraries have no IR provider or one of the known and supported IR providers.
 */
private fun KlibLoaderResult.checkForUnknownIrProviders(): KlibLoaderResult {
    if (librariesStdlibFirst.isEmpty()) return this

    val librariesWithoutOrWithKnownIrProvider = ArrayList<KotlinLibrary>(librariesStdlibFirst.size)
    val problematicLibrariesWithUnknownIrProvider = ArrayList<ProblematicLibrary>()

    librariesStdlibFirst.forEach { library ->
        when (val irProviderName = library.irProviderName) {
            null, KLIB_INTEROP_IR_PROVIDER_IDENTIFIER -> librariesWithoutOrWithKnownIrProvider += library
            else -> problematicLibrariesWithUnknownIrProvider += ProblematicLibrary(
                libraryPath = library.path.pathString,
                problemCase = UnknownIrProvider(irProviderName)
            )
        }
    }

    if (problematicLibrariesWithUnknownIrProvider.isEmpty()) return this

    return KlibLoaderResult(
        librariesStdlibFirst = librariesWithoutOrWithKnownIrProvider,
        problematicLibraries = problematicLibraries + problematicLibrariesWithUnknownIrProvider,
    )
}

private class UnknownIrProvider(val irProviderName: String) : OtherCheckMismatch() {
    override val caption get() = "Library with unsupported IR provider $irProviderName"
    override val details get() = "Only libraries without IR provider or with $KLIB_INTEROP_IR_PROVIDER_IDENTIFIER IR provider are supported."
}

/**
 * Select only those libraries that are eligible to be "included":
 * - each library should not be a C-interop library
 * - also there should not be libraries from the Kotlin/Native distribution
 */
private fun List<KotlinLibrary>.selectLibrariesEligibleToBeIncluded(configuration: CompilerConfiguration): List<KotlinLibrary> {
    if (isEmpty()) return this

    fun reportIssue(library: KotlinLibrary, libraryKind: String) = configuration.report(
        factory = if (configuration.testEnvironment)
            SerializationErrors.KLIB_LOADING_ERROR
        else
            SerializationErrors.KLIB_LOADING_WARNING,
        message = "KLIB loader: $libraryKind cannot be used in ${K2NativeCompilerArguments::includes.cliArgument} CLI argument: ${library.path}"
    )

    return mapNotNull { library ->
        if (library.isFromKotlinNativeDistribution) {
            reportIssue(library, libraryKind = "Library from the Kotlin/Native distribution")
            return@mapNotNull null
        } else if (library.isCInteropLibrary()) {
            reportIssue(library, libraryKind = "C-interop library")
            return@mapNotNull null
        }

        library
    }
}
