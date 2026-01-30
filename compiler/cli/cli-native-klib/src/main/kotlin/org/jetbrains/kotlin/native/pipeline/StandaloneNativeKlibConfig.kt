/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult

class StandaloneNativeKlibConfig(
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    override val resolvedLibraries: KotlinLibraryResolveResult,
) : NativeKlibCompilationConfig {

    override val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: File(outputPath).name

    override val manifestProperties: Properties?
        get() = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
            File(it).loadProperties()
        }
}
