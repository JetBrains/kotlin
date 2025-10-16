/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
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
        get() = configuration.get(KonanConfigKeys.PRODUCE)!!

    val metadataKlib: Boolean
        get() = configuration.getBoolean(CommonConfigurationKeys.METADATA_KLIB)

    val headerKlibPath: String?
        get() = configuration.get(KonanConfigKeys.HEADER_KLIB)?.removeSuffixIfPresent(".klib")

    val friendModuleFiles: Set<File>
        get() = configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map { File(it) }?.toSet() ?: emptySet()

    val refinesModuleFiles: Set<File>
        get() = configuration.get(KonanConfigKeys.REFINES_MODULES)?.map { File(it) }?.toSet().orEmpty()

    val nativeLibraries: List<String>
        get() = configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    val includeBinaries: List<String>
        get() = configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    val writeDependenciesOfProducedKlibTo: String?
        get() = configuration.get(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)

    val nativeTargetsForManifest: Collection<KonanTarget>?
        get() = configuration.get(KonanConfigKeys.MANIFEST_NATIVE_TARGETS)

    val manifestProperties: Properties?

    val shortModuleName: String?
        get() = configuration.get(KonanConfigKeys.SHORT_MODULE_NAME)

    val outputPath: String
        get() = configuration.get(KonanConfigKeys.OUTPUT)?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName
}