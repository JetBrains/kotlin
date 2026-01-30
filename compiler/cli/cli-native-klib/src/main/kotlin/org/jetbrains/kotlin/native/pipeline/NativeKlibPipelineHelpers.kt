/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys.KONAN_DATA_DIR
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.native.KonanLibrariesResolveSupport
import org.jetbrains.kotlin.utils.KotlinNativePaths

internal fun createStandaloneConfig(configuration: CompilerConfiguration): NativeKlibCompilationConfig {
    val targetName = configuration.get(KonanConfigKeys.TARGET)
    val target = if (targetName != null) {
        KonanTarget.predefinedTargets[targetName]
            ?: error("Unknown target: $targetName")
    } else {
        HostManager.host
    }
    val distribution = Distribution(KotlinNativePaths.homePath.absolutePath, konanDataDir = configuration.get(KONAN_DATA_DIR))
    val resolver = KonanLibrariesResolveSupport(
        configuration, target, distribution, resolveManifestDependenciesLenient = true
    )
    return StandaloneNativeKlibConfig(
        configuration = configuration,
        target = target,
        resolvedLibraries = resolver.resolvedLibraries,
    )
}

internal val CompilerConfiguration.phaseConfig: PhaseConfig?
    get() = this.get(CommonConfigurationKeys.PHASE_CONFIG)
