/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.arguments.CommonNativeCompilerArguments
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File

/**
 * Sets up configuration from arguments common to all native compilation modes (full compilation and klib-only).
 * This handles arguments from [CommonNativeCompilerArguments].
 *
 * @param nostdlib Value for [KonanConfigKeys.NOSTDLIB]. The full native pipeline computes this based on `libraryToAddToCache`.
 * @param nodefaultlibs Value for [KonanConfigKeys.NODEFAULTLIBS]. The full native pipeline computes this based on `libraryToAddToCache`.
 * @param noendorsedlibs Value for [KonanConfigKeys.NOENDORSEDLIBS]. The full native pipeline computes this based on `libraryToAddToCache`.
 * @param kotlinHome Value for [KonanConfigKeys.KONAN_HOME].
 */
fun CompilerConfiguration.setupCommonNativeArguments(
    arguments: CommonNativeCompilerArguments,
    nostdlib: Boolean,
    nodefaultlibs: Boolean,
    noendorsedlibs: Boolean,
    kotlinHome: String?,
) {
    put(KonanConfigKeys.NOSTDLIB, nostdlib)
    put(KonanConfigKeys.NODEFAULTLIBS, nodefaultlibs)
    @Suppress("DEPRECATION")
    put(KonanConfigKeys.NOENDORSEDLIBS, noendorsedlibs)
    kotlinHome?.let { put(KonanConfigKeys.KONAN_HOME, it) }
    arguments.headerKlibPath?.let { put(KonanConfigKeys.HEADER_KLIB, it) }
    arguments.target?.let { put(KonanConfigKeys.TARGET, it) }
    arguments.moduleName?.let { put(KonanConfigKeys.MODULE_NAME, it) }
    put(KonanConfigKeys.LIBRARY_FILES, arguments.libraries?.toList() ?: emptyList())
    put(KonanConfigKeys.NOPACK, arguments.nopack)
    arguments.outputName?.let { put(KonanConfigKeys.OUTPUT, it) }
    arguments.manifestFile?.let { put(KonanConfigKeys.MANIFEST_FILE, it) }
    put(KonanConfigKeys.INCLUDED_BINARY_FILES, arguments.includeBinaries?.toList() ?: emptyList())
    put(KonanConfigKeys.INCLUDED_LIBRARIES, arguments.includes?.toList() ?: emptyList())
    arguments.shortModuleName?.let { put(KonanConfigKeys.SHORT_MODULE_NAME, it) }
    arguments.friendModules?.let { friendModulesString ->
        put(KonanConfigKeys.FRIEND_MODULES, friendModulesString.split(File.pathSeparator).filterNot(String::isEmpty))
    }
    arguments.refinesPaths?.let { refinesPaths ->
        put(KonanConfigKeys.REFINES_MODULES, refinesPaths.filterNot(String::isEmpty))
    }
    put(KonanConfigKeys.EXPORT_KDOC, arguments.exportKDoc)
    arguments.writeDependenciesOfProducedKlibTo?.let { put(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO, it) }
}
