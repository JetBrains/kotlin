/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.metadataKlib
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.config.konanHome
import org.jetbrains.kotlin.konan.config.konanLibraries
import org.jetbrains.kotlin.konan.config.konanNoDefaultLibs
import org.jetbrains.kotlin.konan.config.konanNoStdlib
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.library.KlibNativeDistributionLibraryProvider
import org.jetbrains.kotlin.konan.library.KlibNativeManifestTransformer
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLibraryProvider
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.utils.KotlinNativePaths
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

/**
 * This is the entry point to load Kotlin/Native KLIBs in the test pipeline.
 *
 * @param configuration The current compiler configuration.
 */
fun loadNativeKlibsInProductionPipeline(configuration: CompilerConfiguration): LoadedKlibs {
    val nativeTarget: KonanTarget = HostManager().targetManager(configuration.konanTarget).target

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
 * @param runtimeLibraryProviders List of library providers to load runtime libraries.
 *   Note: It is essential to load libraries from the Kotlin/Native distribution through a special provider
 *   that marks such libraries with `Klib.isFromKotlinNativeDistribution` flag.
 * @param nativeTarget The Kotlin/Native specific target (it's used with [KlibPlatformChecker.Native] to avoid
 *  loading KLIBs for the wrong platform and the wrong Kotlin/Native target).
 */
fun loadNativeKlibsInTestPipeline(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    runtimeLibraryProviders: List<KlibLibraryProvider> = emptyList(),
    nativeTarget: KonanTarget,
): LoadedKlibs = loadNativeKlibs(
    configuration = configuration,
    runtimeLibraryProviders = runtimeLibraryProviders,
    libraryPaths = libraryPaths,
    platformChecker = KlibPlatformChecker.Native(nativeTarget.name),
    nativeTarget = nativeTarget,
    useStricterChecks = true,
)

private fun loadNativeKlibs(
    configuration: CompilerConfiguration,
    runtimeLibraryProviders: List<KlibLibraryProvider>,
    libraryPaths: List<String>,
    platformChecker: KlibPlatformChecker,
    nativeTarget: KonanTarget,
    useStricterChecks: Boolean,
): LoadedKlibs {
    val result = KlibLoader {
        libraryProviders(runtimeLibraryProviders)
        libraryPaths(libraryPaths)
        platformChecker(platformChecker)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it) }
        manifestTransformer(KlibNativeManifestTransformer(nativeTarget))
    }.load()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = useStricterChecks) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    return LoadedKlibs(all = result.librariesStdlibFirst)
}
