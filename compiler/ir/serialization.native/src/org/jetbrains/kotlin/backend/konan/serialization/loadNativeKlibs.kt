/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.loadFriendLibraries
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.klibAbiCompatibilityLevel
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.metadataKlib
import org.jetbrains.kotlin.config.skipLibrarySpecialCompatibilityChecks
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.config.*
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.library.KlibNativeManifestTransformer
import org.jetbrains.kotlin.konan.library.isFromKotlinNativeDistribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.irProviderName
import org.jetbrains.kotlin.library.loader.KlibLibraryProvider
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibLoaderResult
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.OtherCheckMismatch
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblematicLibrary
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

/**
 * This is the entry point to load Kotlin/Native KLIBs in the production pipeline.
 *
 * @param configuration The current compiler configuration.
 * @param nativeTarget The Kotlin/Native-specific target.
 */
fun loadNativeKlibsInProductionPipeline(
    configuration: CompilerConfiguration,
    nativeTarget: KonanTarget,
): LoadedNativeKlibs {
    val loadStdlib = !configuration.konanNoStdlib
    val loadPlatformLibs = !configuration.konanNoDefaultLibs

    val distributionLibrariesProvider: KlibNativeDistributionLibraryProvider? = runIf(loadStdlib || loadPlatformLibs) {
        val nativeHome = configuration.konanHome?.let(::File) ?: KotlinNativePaths.homePath
        KlibNativeDistributionLibraryProvider(nativeHome.absoluteFile) {
            runIf(loadStdlib) { withStdlib() }
            runIf(loadPlatformLibs) { withPlatformLibs(nativeTarget) }
        }
    }

    val platformChecker = if (configuration.metadataKlib)
        KlibPlatformChecker.NativeMetadata(nativeTarget.name)
    else
        KlibPlatformChecker.Native(nativeTarget.name)

    return loadNativeKlibs(
        configuration = configuration,
        runtimeLibraryProviders = listOfNotNull(distributionLibrariesProvider),
        libraryPaths = configuration.konanLibraries,
        friendPaths = configuration.konanFriendLibraries,
        includedPaths = configuration.konanIncludedLibraries,
        platformChecker = platformChecker,
        nativeTarget = nativeTarget,
        useStricterChecks = false,
    )
}

/**
 * This is the entry point to load Kotlin/Native KLIBs in the test pipeline.
 *
 * @param configuration The current compiler configuration.
 * @param libraryPaths Paths of libraries to load.
 * @param friendPaths Paths of friend libraries to load.
 *   Note: It is assumed that [friendPaths] are already included in [libraryPaths].
 * @param includedPaths Paths of the libraries to process as the included modules.
 *   Note: It is assumed that [includedPaths] is already included in [libraryPaths].
 * @param runtimeLibraryProviders List of library providers to load runtime libraries.
 *   Note: It is essential to load libraries from the Kotlin/Native distribution through a special provider
 *   that marks such libraries with [Klib.isFromKotlinNativeDistribution] flag.
 * @param nativeTarget The Kotlin/Native specific target (it's used with [KlibPlatformChecker.Native] to avoid
 *  loading KLIBs for the wrong platform and the wrong Kotlin/Native target).
 */
fun loadNativeKlibsInTestPipeline(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    friendPaths: List<String> = emptyList(),
    includedPaths: List<String> = emptyList(),
    runtimeLibraryProviders: List<KlibLibraryProvider> = emptyList(),
    nativeTarget: KonanTarget
): LoadedNativeKlibs = loadNativeKlibs(
    configuration = configuration,
    libraryPaths = libraryPaths,
    friendPaths = friendPaths,
    includedPaths = includedPaths,
    runtimeLibraryProviders = runtimeLibraryProviders,
    platformChecker = KlibPlatformChecker.Native(nativeTarget.name),
    nativeTarget = nativeTarget,
    useStricterChecks = true
)

private fun loadNativeKlibs(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    friendPaths: List<String>,
    includedPaths: List<String>,
    runtimeLibraryProviders: List<KlibLibraryProvider>,
    platformChecker: KlibPlatformChecker,
    nativeTarget: KonanTarget,
    useStricterChecks: Boolean,
): LoadedNativeKlibs {
    val result = KlibLoader {
        libraryProviders(runtimeLibraryProviders)
        libraryPaths(libraryPaths)
        platformChecker(platformChecker)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it) }
        manifestTransformer(KlibNativeManifestTransformer(nativeTarget))
    }.load()
        .checkForUnknownIrProviders()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = useStricterChecks) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    return LoadedNativeKlibs(
        all = result.librariesStdlibFirst,
        friends = result.loadFriendLibraries(friendPaths),
        included = result.loadFriendLibraries(includedPaths),
    ).also { klibs ->
        if (!configuration.skipLibrarySpecialCompatibilityChecks) {
            KonanLibrarySpecialCompatibilityChecker.check(
                klibs.all, configuration.messageCollector, configuration.klibAbiCompatibilityLevel
            )
        }
    }
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
                libraryPath = library.libraryFile.path,
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
