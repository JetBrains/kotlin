/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.NativeKlibCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.util.PerformanceManager

class NativeKlibConfig(
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    override val resolvedLibraries: KotlinLibraryResolveResult,
) : NativeKlibCompilationConfig {

    override val moduleId: String
        get() = configuration.get(CommonConfigurationKeys.MODULE_NAME) ?: File(outputPath).name

    override val manifestProperties: Properties?
        get() = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
            File(it).loadProperties()
        }
}

class NativePhaseContext(
    override val config: NativeKlibConfig,
) : PhaseContext {
    override var inVerbosePhase: Boolean = false

    override val messageCollector: MessageCollector
        get() = config.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager

    override fun dispose() {}
}