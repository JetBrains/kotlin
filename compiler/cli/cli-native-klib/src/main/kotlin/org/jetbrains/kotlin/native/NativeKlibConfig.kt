/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.NativePhaseContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.konan.config.konanDataDir
import org.jetbrains.kotlin.konan.config.konanManifestAddend
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.native.resolve.KonanLibrariesResolveSupport
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.utils.KotlinNativePaths

class NativeKlibConfig(
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    override val resolvedLibraries: KotlinLibraryResolveResult,
) : NativeKlibCompilationConfig {

    override val moduleId: String
        get() = configuration.moduleName ?: File(outputPath).name

    override val manifestProperties: Properties?
        get() = configuration.konanManifestAddend?.let {
            File(it).loadProperties()
        }
}

class NativeFirstStagePhaseContext(
    override val config: NativeKlibConfig,
) : NativePhaseContext {
    override var inVerbosePhase: Boolean = false

    override val messageCollector: MessageCollector
        get() = config.configuration.messageCollector

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager

    override fun dispose() {}
}

internal fun createNativeKlibConfig(configuration: CompilerConfiguration): NativeKlibConfig {
    val targetName = configuration.konanTarget
    val target = if (targetName != null) {
        KonanTarget.predefinedTargets[targetName]
            ?: error("Unknown target: $targetName")
    } else {
        HostManager.host
    }
    val distribution = Distribution(KotlinNativePaths.homePath.absolutePath, konanDataDir = configuration.konanDataDir)
    val resolver = KonanLibrariesResolveSupport(
        configuration, target, distribution, resolveManifestDependenciesLenient = true
    )
    return NativeKlibConfig(
        configuration = configuration,
        target = target,
        resolvedLibraries = resolver.resolvedLibraries,
    )
}