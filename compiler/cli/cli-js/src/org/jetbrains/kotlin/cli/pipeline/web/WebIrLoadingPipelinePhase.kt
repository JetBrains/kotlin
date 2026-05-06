/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.cli.js.platformChecker
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.MainModule.Klib
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.loadIr
import org.jetbrains.kotlin.ir.backend.js.loadWebKlibs
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.js.config.includes
import org.jetbrains.kotlin.js.config.libraries
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.io.File

abstract class WebIrLoadingPipelinePhase(
    name: String
) : PipelinePhase<ConfigurationPipelineArtifact, WebLoadedIrPipelineArtifact>(
    name,
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished),
) {
    protected abstract fun createIrFactory(): IrFactory

    protected open fun loadIr(
        configuration: CompilerConfiguration,
        irFactory: IrFactory,
        modulesStructure: ModulesStructure,
    ): IrModuleInfo = loadIr(modulesStructure, irFactory)

    override fun executePhase(input: ConfigurationPipelineArtifact): WebLoadedIrPipelineArtifact {
        val configuration = input.configuration
        val includes = configuration.includes!!
        val includesPath = File(includes).canonicalPath
        val mainLibPath = configuration.libraries.find { File(it).canonicalPath == includesPath }
            ?: error("No library with name $includes ($includesPath) found")
        val kLib = Klib(mainLibPath)
        val klibs = loadWebKlibs(configuration, configuration.platformChecker)
        val module = ModulesStructure(
            mainModule = kLib,
            compilerConfiguration = configuration,
            klibs = klibs,
        )
        val irFactory = createIrFactory()
        val loadedIr = configuration.perfManager.tryMeasurePhaseTime(PhaseType.IrLinking) {
            loadIr(configuration, irFactory, module)
        }
        return WebLoadedIrPipelineArtifact(loadedIr, module, configuration)
    }
}
