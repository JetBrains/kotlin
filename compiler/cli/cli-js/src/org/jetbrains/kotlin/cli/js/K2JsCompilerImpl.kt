/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.pipeline.web.js.JsBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.js.JsConfigurationUpdater
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsCodeGenerator
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

class Ir2JsTransformer private constructor(
    val module: ModulesStructure,
    val messageCollector: MessageCollector,
    val mainCallArguments: List<String>?,
    val keep: Set<String>,
    val dceRuntimeDiagnostic: String?,
    val safeExternalBoolean: Boolean,
    val safeExternalBooleanDiagnostic: String?,
    val granularity: JsGenerationGranularity,
    val dce: Boolean,
    val minimizedMemberNames: Boolean,
) {
    constructor(
        arguments: K2JSCompilerArguments,
        module: ModulesStructure,
        messageCollector: MessageCollector,
        mainCallArguments: List<String>?,
    ) : this(
        module,
        messageCollector,
        mainCallArguments,
        keep = arguments.irKeep?.split(",")?.filterNot { it.isEmpty() }?.toSet() ?: emptySet(),
        dceRuntimeDiagnostic = arguments.irDceRuntimeDiagnostic,
        safeExternalBoolean = arguments.irSafeExternalBoolean,
        safeExternalBooleanDiagnostic = arguments.irSafeExternalBooleanDiagnostic,
        granularity = arguments.granularity,
        dce = arguments.irDce,
        minimizedMemberNames = arguments.irMinimizedMemberNames,
    )

    constructor(
        configuration: CompilerConfiguration,
        module: ModulesStructure,
        messageCollector: MessageCollector,
        mainCallArguments: List<String>?,
    ) : this(
        module,
        messageCollector,
        mainCallArguments,
        keep = configuration.keep.toSet(),
        dceRuntimeDiagnostic = configuration.dceRuntimeDiagnostic,
        safeExternalBoolean = configuration.safeExternalBoolean,
        safeExternalBooleanDiagnostic = configuration.safeExternalBooleanDiagnostic,
        granularity = configuration.granularity!!,
        dce = configuration.dce,
        minimizedMemberNames = configuration.minimizedMemberNames,
    )

    private val performanceManager = module.compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]

    private fun lowerIr(): LoweredIr {
        return compile(
            mainCallArguments,
            module,
            IrFactoryImplForJsIC(WholeWorldStageController()),
            keep = keep,
            dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                dceRuntimeDiagnostic,
                messageCollector
            ),
            safeExternalBoolean = safeExternalBoolean,
            safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                safeExternalBooleanDiagnostic,
                messageCollector
            ),
            granularity = granularity,
        )
    }

    private fun makeJsCodeGenerator(): JsCodeGenerator {
        val ir = lowerIr()
        val transformer = IrModuleToJsTransformer(ir.context, ir.moduleFragmentToUniqueName, mainCallArguments != null)

        val mode = TranslationMode.fromFlags(dce, granularity, minimizedMemberNames)
        return transformer
            .also { performanceManager?.notifyBackendGenerationStarted() }
            .makeJsCodeGenerator(ir.allModules, mode)
    }

    fun compileAndTransformIrNew(): CompilationOutputsBuilt {
        return makeJsCodeGenerator()
            .generateJsCode(relativeRequirePath = true, outJsProgram = false)
            .also {
                performanceManager?.notifyBackendGenerationFinished()
            }
    }
}

internal class K2JsCompilerImpl(
    arguments: K2JSCompilerArguments,
    configuration: CompilerConfiguration,
    moduleName: String,
    outputName: String,
    outputDir: File,
    messageCollector: MessageCollector,
    performanceManager: PerformanceManager?,
) : K2JsCompilerImplBase(
    arguments = arguments,
    configuration = configuration,
    moduleName = moduleName,
    outputName = outputName,
    outputDir = outputDir,
    messageCollector = messageCollector,
    performanceManager = performanceManager
) {
    override fun checkTargetArguments(): ExitCode? {
        if (arguments.targetVersion == null) {
            messageCollector.report(ERROR, "Unsupported ECMA version: ${arguments.target}")
            return COMPILATION_ERROR
        }

        if (arguments.script) {
            messageCollector.report(ERROR, "K/JS does not support Kotlin script (*.kts) files")
            return COMPILATION_ERROR
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                return OK
            }
            if (arguments.includes.isNullOrEmpty()) {
                messageCollector.report(ERROR, "Specify at least one source file or directory", null)
                return COMPILATION_ERROR
            }
        }

        return null
    }

    override fun tryInitializeCompiler(rootDisposable: Disposable): KotlinCoreEnvironment? {
        JsConfigurationUpdater.fillConfiguration(configuration, arguments)
        if (messageCollector.hasErrors()) return null

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        val sourcesFiles = environmentForJS.getSourceFiles()
        if (sourcesFiles.isEmpty() && (!incrementalCompilationIsEnabledForJs(arguments)) && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return null
        }

        performanceManager?.apply {
            targetDescription = "$moduleName-${configuration.moduleKind}"
            addSourcesStats(sourcesFiles.size, environmentForJS.countLinesOfCode(sourcesFiles))
            notifyCompilerInitialized()
        }

        return environmentForJS
    }

    override fun compileWithIC(
        icCaches: IcCachesArtifacts,
        targetConfiguration: CompilerConfiguration,
        moduleKind: ModuleKind?
    ): ExitCode {
        JsBackendPipelinePhase.compileIncrementally(
            icCaches,
            configuration,
            moduleKind ?: return INTERNAL_ERROR,
            moduleName,
            outputDir,
            outputName,
            arguments.granularity,
            arguments.dtsStrategy
        )
        performanceManager?.notifyIRGenerationFinished()
        return OK
    }

    override fun compileNoIC(mainCallArguments: List<String>?, module: ModulesStructure, moduleKind: ModuleKind?): ExitCode {
        if (!arguments.irProduceJs) {
            performanceManager?.notifyIRGenerationFinished()
            return OK
        }

        JsConfigurationUpdater.checkWasmArgumentsUsage(arguments, messageCollector)

        configuration.phaseConfig = createPhaseConfig(arguments).also {
            if (arguments.listPhases) it.list(getJsLowerings(configuration))
        }
        val ir2JsTransformer = Ir2JsTransformer(arguments, module, messageCollector, mainCallArguments)
        val outputs = JsBackendPipelinePhase.compileNonIncrementally(
            messageCollector,
            ir2JsTransformer,
            // moduleKind for JS compilation is always not null (see [JsConfigurationUpdater.fillConfiguration])
            moduleKind!!,
            moduleName,
            outputDir,
            outputName,
            arguments.dtsStrategy
        )

        return if (outputs != null) OK else INTERNAL_ERROR
    }
}
