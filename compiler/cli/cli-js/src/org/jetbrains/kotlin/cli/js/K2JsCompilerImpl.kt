/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
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
import java.io.File
import java.io.IOException

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
            .also { performanceManager?.notifyIRGenerationStarted() }
            .makeJsCodeGenerator(ir.allModules, mode)
    }

    fun compileAndTransformIrNew(): CompilationOutputsBuilt {
        return makeJsCodeGenerator()
            .generateJsCode(relativeRequirePath = true, outJsProgram = false)
            .also {
                performanceManager?.notifyIRGenerationFinished()
                performanceManager?.notifyGenerationFinished()
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
    performanceManager: CommonCompilerPerformanceManager?,
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

    override fun tryInitializeCompiler(libraries: List<String>, rootDisposable: Disposable): KotlinCoreEnvironment? {
        initializeCommonConfiguration(libraries)

        val targetVersion = arguments.targetVersion?.also {
            configuration.put(JSConfigurationKeys.TARGET, it)
        }

        configuration.put(JSConfigurationKeys.OPTIMIZE_GENERATED_JS, arguments.optimizeGeneratedJs)

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        if (messageCollector.hasErrors()) return null

        val configurationJs = environmentForJS.configuration
        val sourcesFiles = environmentForJS.getSourceFiles()
        val isES2015 = targetVersion == EcmaVersion.es2015
        val moduleKind = configuration[JSConfigurationKeys.MODULE_KIND]
            ?: moduleKindMap[arguments.moduleKind]
            ?: ModuleKind.ES.takeIf { isES2015 }
            ?: ModuleKind.UMD

        configurationJs.put(JSConfigurationKeys.MODULE_KIND, moduleKind)
        configurationJs.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configurationJs.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)
        configurationJs.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, arguments.irPropertyLazyInitialization)
        configurationJs.put(JSConfigurationKeys.GENERATE_POLYFILLS, arguments.generatePolyfills)
        configurationJs.put(JSConfigurationKeys.GENERATE_DTS, arguments.generateDts)
        configurationJs.put(JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS, arguments.irGenerateInlineAnonymousFunctions)
        configurationJs.put(JSConfigurationKeys.USE_ES6_CLASSES, arguments.useEsClasses ?: isES2015)
        configurationJs.put(JSConfigurationKeys.COMPILE_SUSPEND_AS_JS_GENERATOR, arguments.useEsGenerators ?: isES2015)
        configurationJs.put(JSConfigurationKeys.COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS, arguments.useEsArrowFunctions ?: isES2015)

        arguments.platformArgumentsProviderJsExpression?.let {
            configurationJs.put(JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS, it)
        }

        val moduleName = arguments.irModuleName ?: outputName
        configurationJs.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        try {
            configurationJs.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return null
        }

        if (sourcesFiles.isEmpty() && (!incrementalCompilationIsEnabledForJs(arguments)) && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return null
        }

        performanceManager?.notifyCompilerInitialized(
            sourcesFiles.size, environmentForJS.countLinesOfCode(sourcesFiles), "$moduleName-$moduleKind"
        )

        return environmentForJS
    }

    override fun compileWithIC(
        icCaches: IcCachesArtifacts,
        targetConfiguration: CompilerConfiguration,
        moduleKind: ModuleKind?
    ): ExitCode {

        val moduleKind = moduleKind ?: return INTERNAL_ERROR

        val beforeIc2Js = System.currentTimeMillis()

        val jsArtifacts = icCaches.artifacts.filterIsInstance<JsModuleArtifact>()
        val jsExecutableProducer = JsExecutableProducer(
            mainModuleName = moduleName,
            moduleKind = moduleKind,
            sourceMapsInfo = SourceMapsInfo.from(targetConfiguration),
            caches = jsArtifacts,
            relativeRequirePath = true
        )
        val (outputs, rebuiltModules) = jsExecutableProducer.buildExecutable(arguments.granularity, outJsProgram = false)
        outputs.writeAll(outputDir, outputName, arguments.dtsStrategy, moduleName, moduleKind)

        performanceManager?.notifyIRTranslationFinished()

        messageCollector.report(INFO, "Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
        for ((event, duration) in jsExecutableProducer.getStopwatchLaps()) {
            messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
        }

        for (module in rebuiltModules) {
            messageCollector.report(INFO, "IC module builder rebuilt JS for module [${File(module).name}]")
        }

        return OK
    }

    override fun compileNoIC(mainCallArguments: List<String>?, module: ModulesStructure, moduleKind: ModuleKind?): ExitCode {
        if (!arguments.irProduceJs) {
            performanceManager?.notifyIRTranslationFinished()
            return OK
        }

        val moduleKind = moduleKind ?: return INTERNAL_ERROR

        if (arguments.irDceDumpReachabilityInfoToFile != null) {
            messageCollector.report(STRONG_WARNING, "Dumping the reachability info to file is not supported for Kotlin/Js.")
        }
        if (arguments.irDceDumpDeclarationIrSizesToFile != null) {
            messageCollector.report(STRONG_WARNING, "Dumping the size of declarations to file is not supported for Kotlin/Js.")
        }

        configuration.phaseConfig = createPhaseConfig(arguments).also {
            if (arguments.listPhases) it.list(getJsPhases(configuration))
        }

        val start = System.currentTimeMillis()

        try {
            val ir2JsTransformer = Ir2JsTransformer(arguments, module, messageCollector, mainCallArguments)
            val outputs = ir2JsTransformer.compileAndTransformIrNew()

            messageCollector.report(INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")

            outputs.writeAll(outputDir, outputName, arguments.dtsStrategy, moduleName, moduleKind)
        } catch (e: CompilationException) {
            messageCollector.report(
                ERROR,
                e.stackTraceToString(),
                CompilerMessageLocation.create(
                    path = e.path,
                    line = e.line,
                    column = e.column,
                    lineContent = e.content
                )
            )
            return INTERNAL_ERROR
        }

        return OK
    }
}
