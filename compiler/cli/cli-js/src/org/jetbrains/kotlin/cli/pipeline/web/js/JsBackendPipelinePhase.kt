/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.js

import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.cli.CliDiagnostics.COMPILER_EXCEPTION
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.js.IcCachesArtifacts
import org.jetbrains.kotlin.cli.js.Ir2JsTransformer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.pipeline.web.JsBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WebBackendPipelinePhase
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.ic.JsModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.js.config.WebArtifactConfiguration
import org.jetbrains.kotlin.js.config.artifactConfiguration
import org.jetbrains.kotlin.js.config.outputDir
import java.io.File

object JsBackendPipelinePhase : WebBackendPipelinePhase<JsBackendPipelineArtifact, JsBackendPipelineArtifact>("JsBackendPipelinePhase") {
    override val configFiles: EnvironmentConfigFiles
        get() = EnvironmentConfigFiles.JS_CONFIG_FILES

    override fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact {
        val outputs = compileIncrementally(
            icCaches,
            configuration,
            configuration.artifactConfiguration!!,
        )
        return JsBackendPipelineArtifact(outputs, configuration.outputDir!!, configuration)
    }

    private fun compileIncrementally(
        icCaches: IcCachesArtifacts,
        configuration: CompilerConfiguration,
        artifactConfiguration: WebArtifactConfiguration,
    ): CompilationOutputs {
        val beforeIc2Js = System.currentTimeMillis()

        val jsArtifacts = icCaches.artifacts.filterIsInstance<JsModuleArtifact>()
        val jsExecutableProducer = JsExecutableProducer(
            mainModuleName = artifactConfiguration.moduleName,
            moduleKind = artifactConfiguration.moduleKind,
            sourceMapsInfo = SourceMapsInfo.from(configuration),
            caches = jsArtifacts,
            relativeRequirePath = true
        )
        val (outputs, rebuiltModules) = jsExecutableProducer.buildExecutable(artifactConfiguration.granularity, outJsProgram = false)
        outputs.writeAll(artifactConfiguration)

        configuration.reportLog("Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
        for ((event, duration) in jsExecutableProducer.getStopwatchLaps()) {
            configuration.reportLog("  $event: ${(duration / 1e6).toInt()}ms")
        }

        for (module in rebuiltModules) {
            configuration.reportLog("IC module builder rebuilt JS for module [${File(module).name}]")
        }
        return outputs
    }

    override fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        mainCallArguments: List<String>?,
    ): JsBackendPipelineArtifact? {
        val ir2JsTransformer = Ir2JsTransformer(configuration, module, configuration.messageCollector, mainCallArguments)
        val outputs = compileNonIncrementally(
            configuration,
            ir2JsTransformer,
            configuration.artifactConfiguration!!,
        ) ?: return null
        return JsBackendPipelineArtifact(outputs, configuration.outputDir!!, configuration)
    }

    override fun compileIntermediate(
        intermediateResult: JsBackendPipelineArtifact,
        configuration: CompilerConfiguration,
    ): JsBackendPipelineArtifact = intermediateResult

    private fun compileNonIncrementally(
        configuration: CompilerConfiguration,
        ir2JsTransformer: Ir2JsTransformer,
        artifactConfiguration: WebArtifactConfiguration,
    ): CompilationOutputs? {
        val start = System.currentTimeMillis()
        try {
            val outputs = ir2JsTransformer.compileAndTransformIrNew()
            configuration.reportLog("Executable production duration: ${System.currentTimeMillis() - start}ms")
            outputs.writeAll(artifactConfiguration)
            return outputs
        } catch (e: CompilationException) {
            configuration.report(
                COMPILER_EXCEPTION,
                e.stackTraceToString(),
                CompilerMessageLocation.create(
                    path = e.path,
                    line = e.line,
                    column = e.column,
                    lineContent = e.content
                )
            )
            return null
        }
    }
}
