/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.konanFriendLibraries
import org.jetbrains.kotlin.konan.config.konanGeneratedHeaderKlibPath
import org.jetbrains.kotlin.konan.config.konanIncludedBinaries
import org.jetbrains.kotlin.konan.config.konanManifestNativeTargets
import org.jetbrains.kotlin.konan.config.konanNativeLibraries
import org.jetbrains.kotlin.konan.config.konanOutputPath
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.config.konanRefinesModules
import org.jetbrains.kotlin.konan.config.konanShortModuleName
import org.jetbrains.kotlin.konan.config.konanWriteDependenciesOfProducedKlibTo
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.util.removeSuffixIfPresent

/**
 * This interface exists not because it is a good abstraction. Rather, it emerged
 * from the need to extract src -> klib compilation from the /kotlin-native directory.
 */
interface NativeKlibCompilationConfig {
    val project: Project

    val configuration: CompilerConfiguration

    val target: KonanTarget

    val resolvedLibraries: KotlinLibraryResolveResult

    val moduleId: String

    val produce: CompilerOutputKind
        get() = configuration.konanProducedArtifactKind!!

    val metadataKlib: Boolean
        get() = configuration.getBoolean(CommonConfigurationKeys.METADATA_KLIB)

    val headerKlibPath: String?
        get() = configuration.konanGeneratedHeaderKlibPath?.removeSuffixIfPresent(".klib")

    val friendModuleFiles: Set<File>
        get() = configuration.konanFriendLibraries.map { File(it) }.toSet()

    val refinesModuleFiles: Set<File>
        get() = configuration.konanRefinesModules.map { File(it) }.toSet()

    val nativeLibraries: List<String>
        get() = configuration.konanNativeLibraries

    val includeBinaries: List<String>
        get() = configuration.konanIncludedBinaries

    val writeDependenciesOfProducedKlibTo: String?
        get() = configuration.konanWriteDependenciesOfProducedKlibTo

    val nativeTargetsForManifest: Collection<KonanTarget>
        get() = configuration.konanManifestNativeTargets

    val manifestProperties: Properties?

    val shortModuleName: String?
        get() = configuration.konanShortModuleName

    val outputPath: String
        get() = configuration.konanOutputPath?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName
}