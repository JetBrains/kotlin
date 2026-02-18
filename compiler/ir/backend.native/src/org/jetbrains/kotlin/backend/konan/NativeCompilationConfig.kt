/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.metadataKlib
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.config.konanFriendLibraries
import org.jetbrains.kotlin.konan.config.konanIncludedBinaries
import org.jetbrains.kotlin.konan.config.konanManifestAddend
import org.jetbrains.kotlin.konan.config.konanNativeLibraries
import org.jetbrains.kotlin.konan.config.konanOutputPath
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.config.konanRefinesModules
import org.jetbrains.kotlin.konan.config.konanShortModuleName
import org.jetbrains.kotlin.konan.config.konanWriteDependenciesOfProducedKlibTo
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.util.removeSuffixIfPresent

/**
 * This abstract class exists not because it is a good abstraction. Rather, it emerged
 * from the need to extract src -> klib compilation from the /kotlin-native directory.
 */
abstract class NativeCompilationConfig {
    abstract val configuration: CompilerConfiguration
    abstract val target: KonanTarget
    abstract val moduleId: String

    abstract val loadedKlibs: LoadedNativeKlibs

    val produce: CompilerOutputKind get() = configuration.konanProducedArtifactKind!!
    val metadataKlib: Boolean get() = configuration.metadataKlib

    val friendModuleFiles: Set<File> by lazy { configuration.konanFriendLibraries.map { File(it) }.toSet() }
    val refinesModuleFiles: Set<File> by lazy { configuration.konanRefinesModules.map { File(it) }.toSet() }
    val nativeLibraries: List<String> get() = configuration.konanNativeLibraries
    val includeBinaries: List<String> get() = configuration.konanIncludedBinaries

    val writeDependenciesOfProducedKlibTo: String? get() = configuration.konanWriteDependenciesOfProducedKlibTo
    val nativeTargetsForManifest: Collection<KonanTarget>? get() = configuration[NativeConfigurationKeys.KONAN_MANIFEST_NATIVE_TARGETS]

    val manifestProperties: Properties? by lazy { configuration.konanManifestAddend?.let { File(it).loadProperties() } }

    val shortModuleName: String? get() = configuration.konanShortModuleName
    val outputPath: String by lazy { configuration.konanOutputPath?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName }
}
