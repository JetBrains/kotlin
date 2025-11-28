/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.loadFriendLibraries
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.zipFileSystemAccessor
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.KlibNativeManifestTransformer
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker

/**
 * This is the entry point to load Kotlin/Native KLIBs in the test pipeline.
 *
 * @param configuration The current compiler configuration.
 * @param libraryPaths Paths of libraries to load.
 * @param friendPaths Paths of friend libraries to load.
 *   Note: It is assumed that [friendPaths] are already included in [libraryPaths].
 * @param includedPath Path of the library to process as the included module.
 *   Note: It is assumed that [includedPath] is already included in [libraryPaths].
 * @param nativeTarget The Kotlin/Native specific target (it's used with [KlibPlatformChecker.Native] to avoid
 *  loading KLIBs for the wrong platform and the wrong Kotlin/Native target).
 */
fun loadNativeKlibsInTestPipeline(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    friendPaths: List<String> = emptyList(),
    includedPath: String? = null,
    nativeTarget: KonanTarget
): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(libraryPaths)
        platformChecker(KlibPlatformChecker.Native(nativeTarget.name))
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it)}
        manifestTransformer(KlibNativeManifestTransformer(nativeTarget))
    }.load()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = true) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    return LoadedKlibs(
        all = result.librariesStdlibFirst,
        friends = result.loadFriendLibraries(friendPaths),
        included = result.loadFriendLibraries(listOfNotNull(includedPath)).firstOrNull()
    )
}
