/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.loadFriendLibraries
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.checkers.JsStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.WasmStandardLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.js.config.zipFileSystemAccessor
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker

/**
 * Kotlin libraries (KLIBs) that were loaded from the file system.
 *
 * @property all The full list of loaded [KotlinLibrary]s.
 *  This list consists of KLIBs who's paths were passed via CLI using options `-libraries` and `-Xinclude`.
 *  The order of elements in the list preserves the order in which KLIBs were specified in CLI, with the
 *  exception for stdlib, which must go the first in the list. Included libraries go the last.
 *
 * @property friends Only KLIBs having status of "friends" (-Xfriend-modules CLI option).
 *  Note: All [friends] are also included into [all].
 *
 * @property included Only the included KLIB (-Xinclude CLI option), if there was any.
 *  Note: [included] is also in [all].
 */
class LoadedKlibs(
    val all: List<KotlinLibrary>,
    val friends: List<KotlinLibrary> = emptyList(),
    val included: KotlinLibrary? = null
)

/**
 * This is the entry point to load Kotlin/JS or Kotlin/Wasm KLIBs in the production pipeline.
 * All library paths are read from the corresponding parameters of [CompilerConfiguration].
 *
 * @param configuration The current compiler configuration.
 * @param platformChecker The platform checker (it's necessary to avoid loading KLIBs for the wrong platform).
 */
fun loadWebKlibsInProductionPipeline(
    configuration: CompilerConfiguration,
    platformChecker: KlibPlatformChecker,
): LoadedKlibs {
    val klibs = loadWebKlibs(
        configuration = configuration,
        libraryPaths = configuration.libraries,
        friendPaths = configuration.friendLibraries,
        includedPath = configuration.includes,
        platformChecker = platformChecker,
        useStricterChecks = false // That's only necessary in tests. So, false.
    )

    val isWasm = platformChecker is KlibPlatformChecker.Wasm
    val stdlibChecker = if (isWasm) WasmStandardLibrarySpecialCompatibilityChecker else JsStandardLibrarySpecialCompatibilityChecker
    stdlibChecker.check(klibs.all, configuration.messageCollector)

    return klibs
}

/**
 * This is the entry point to load Kotlin/JS or Kotlin/Wasm KLIBs in the test pipeline.
 *
 * @param configuration The current compiler configuration.
 * @param libraryPaths Paths of libraries to load.
 * @param friendPaths Paths of friend libraries to load.
 *   Note: It is assumed that [friendPaths] are already included in [libraryPaths].
 * @param includedPath Path of the library to process as the included module.
 *   Note: It is assumed that [includedPath] is already included in [libraryPaths].
 * @param platformChecker The platform checker (it's necessary to avoid loading KLIBs for the wrong platform).
 */
fun loadWebKlibsInTestPipeline(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    friendPaths: List<String> = emptyList(),
    includedPath: String? = null,
    platformChecker: KlibPlatformChecker,
): LoadedKlibs = loadWebKlibs(
    configuration = configuration,
    libraryPaths = libraryPaths,
    friendPaths = friendPaths,
    includedPath = includedPath,
    platformChecker = platformChecker,
    useStricterChecks = true
)

private fun loadWebKlibs(
    configuration: CompilerConfiguration,
    libraryPaths: List<String>,
    friendPaths: List<String>,
    includedPath: String?,
    platformChecker: KlibPlatformChecker,
    useStricterChecks: Boolean,
): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(libraryPaths)
        platformChecker(platformChecker)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it) }
    }.load()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = useStricterChecks) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    return LoadedKlibs(
        all = result.librariesStdlibFirst,
        friends = result.loadFriendLibraries(friendPaths),
        included = result.loadFriendLibraries(listOfNotNull(includedPath)).firstOrNull()
    )
}
