/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.LoadedKlibs
import org.jetbrains.kotlin.backend.common.eliminateLibrariesWithDuplicatedUniqueNames
import org.jetbrains.kotlin.backend.common.loadFriendLibraries
import org.jetbrains.kotlin.backend.common.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.cli.common.testEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.ir.backend.js.checkers.JsLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.ir.backend.js.checkers.WasmLibrarySpecialCompatibilityChecker
import org.jetbrains.kotlin.js.config.friendLibraries
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker

/**
 * This is the entry point to load Kotlin/JS or Kotlin/Wasm KLIBs.
 * All library paths are read from the corresponding parameters of [CompilerConfiguration].
 *
 * @param configuration The current compiler configuration.
 * @param platformChecker The platform checker (it's necessary to avoid loading KLIBs for the wrong platform).
 */
fun loadWebKlibs(
    configuration: CompilerConfiguration,
    platformChecker: KlibPlatformChecker,
): LoadedKlibs {
    val result = KlibLoader {
        libraryPaths(configuration.libraries)
        platformChecker(platformChecker)
        maxPermittedAbiVersion(KotlinAbiVersion.CURRENT)
        configuration.zipFileSystemAccessor?.let { zipFileSystemAccessor(it) }
    }.load()
        .apply { reportLoadingProblemsIfAny(configuration, allAsErrors = configuration.testEnvironment) }
        // TODO (KT-76785): Handling of duplicated names is a workaround that needs to be removed in the future.
        .eliminateLibrariesWithDuplicatedUniqueNames(configuration)

    return LoadedKlibs(
        all = result.librariesStdlibFirst,
        friends = result.loadFriendLibraries(configuration.friendLibraries),
        included = result.loadFriendLibraries(listOfNotNull(configuration.includes)).firstOrNull()
    ).also { klibs ->
        if (!configuration.skipLibrarySpecialCompatibilityChecks) {
            val isWasm = platformChecker is KlibPlatformChecker.Wasm
            val checker = if (isWasm) WasmLibrarySpecialCompatibilityChecker else JsLibrarySpecialCompatibilityChecker
            checker.check(klibs.all, configuration.messageCollector, configuration.klibAbiCompatibilityLevel)
        }
    }
}

