/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.backend.common.LoadedNativeKlibs
import org.jetbrains.kotlin.backend.konan.NativeCompilationConfig
import org.jetbrains.kotlin.backend.konan.driver.NativePhaseContext
import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibs
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.moduleName
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.konan.config.konanManifestAddend
import org.jetbrains.kotlin.konan.config.konanTarget
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.PerformanceManager

class NativeFirstStageCompilationConfig(
    override val configuration: CompilerConfiguration,
    override val target: KonanTarget,
    val loadedKlibs: LoadedNativeKlibs,
) : NativeCompilationConfig {

    override val moduleId: String
        get() = configuration.moduleName ?: File(outputPath).name

    override val manifestProperties: Properties?
        get() = configuration.konanManifestAddend?.let {
            File(it).loadProperties()
        }

    fun withConfiguration(newConfiguration: CompilerConfiguration): NativeFirstStageCompilationConfig {
        return NativeFirstStageCompilationConfig(newConfiguration, target, loadedKlibs)
    }
}

class NativeFirstStagePhaseContext(
    override val config: NativeFirstStageCompilationConfig,
) : NativePhaseContext {
    override var inVerbosePhase: Boolean = false

    override val diagnosticReporter: IrDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
        config.configuration.diagnosticsCollector,
        config.configuration.languageVersionSettings
    )

    override fun log(message: String) {
        config.configuration.reportLog(message)
    }

    override val performanceManager: PerformanceManager?
        get() = config.configuration.perfManager

    override fun dispose() {}

    fun withConfiguration(newConfiguration: CompilerConfiguration): NativeFirstStagePhaseContext {
        return NativeFirstStagePhaseContext(config.withConfiguration(newConfiguration))
    }
}

internal fun createFirstStageCompilationConfig(configuration: CompilerConfiguration): NativeFirstStageCompilationConfig {
    val targetName = configuration.konanTarget
    val target = if (targetName != null) {
        KonanTarget.predefinedTargets[targetName]
            ?: error("Unknown target: $targetName")
    } else {
        HostManager.host
    }

    return NativeFirstStageCompilationConfig(
        configuration = configuration,
        target = target,
        loadedKlibs = loadNativeKlibs(configuration, target),
    )
}
