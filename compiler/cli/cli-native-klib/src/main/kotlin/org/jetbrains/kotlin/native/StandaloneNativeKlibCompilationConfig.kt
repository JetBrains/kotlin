/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.util.removeSuffixIfPresent

/**
 * A standalone implementation of [NativeKlibCompilationConfig] that does not depend on
 * KonanConfig from kotlin-native/backend.native.
 *
 * This class is used by the CLI pipeline in compiler/cli/cli-native-klib to compile
 * Kotlin sources to Native klibs without requiring the full Native backend.
 */
class StandaloneNativeKlibCompilationConfig(
    override val project: Project,
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    override val resolvedLibraries: KotlinLibraryResolveResult,
) : NativeKlibCompilationConfig {

    override val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME)
            ?: configuration.get(KonanConfigKeys.OUTPUT)?.let { File(it).name }
            ?: produce.visibleName

    override val produce: CompilerOutputKind
        get() = configuration.get(KonanConfigKeys.PRODUCE) ?: CompilerOutputKind.LIBRARY

    override val metadataKlib: Boolean
        get() = configuration.getBoolean(CommonConfigurationKeys.METADATA_KLIB)

    override val headerKlibPath: String?
        get() = configuration.get(KonanConfigKeys.HEADER_KLIB)?.removeSuffixIfPresent(".klib")

    override val friendModuleFiles: Set<File>
        get() = configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map { File(it) }?.toSet() ?: emptySet()

    override val refinesModuleFiles: Set<File>
        get() = configuration.get(KonanConfigKeys.REFINES_MODULES)?.map { File(it) }?.toSet().orEmpty()

    override val nativeLibraries: List<String>
        get() = configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    override val includeBinaries: List<String>
        get() = configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    override val writeDependenciesOfProducedKlibTo: String?
        get() = configuration.get(KonanConfigKeys.WRITE_DEPENDENCIES_OF_PRODUCED_KLIB_TO)

    override val nativeTargetsForManifest: Collection<KonanTarget>?
        get() = configuration.get(KonanConfigKeys.MANIFEST_NATIVE_TARGETS)

    override val manifestProperties = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    override val shortModuleName: String?
        get() = configuration.get(KonanConfigKeys.SHORT_MODULE_NAME)

    override val outputPath: String
        get() = configuration.get(KonanConfigKeys.OUTPUT)?.removeSuffixIfPresent(produce.suffix(target)) ?: produce.visibleName
}
