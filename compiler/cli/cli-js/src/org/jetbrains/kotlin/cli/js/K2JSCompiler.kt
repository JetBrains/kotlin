/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForJSIR
import org.jetbrains.kotlin.cli.js.klib.TopDownAnalyzerFacadeForWasm
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.cli.pipeline.web.CommonWebConfigurationUpdater
import org.jetbrains.kotlin.cli.pipeline.web.WebCliPipeline
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.IncrementalCacheGuard
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File


class K2JSCompiler : CLICompiler<K2JSCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JsPlatforms.defaultJsPlatform

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    override fun doExecutePhased(
        arguments: K2JSCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode? {
        return WebCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    override fun doExecute(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?,
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]

        val outputDirPath = arguments.outputDir
        val outputName = arguments.moduleName
        if (outputDirPath == null) {
            messageCollector.report(ERROR, "IR: Specify output dir via -ir-output-dir", null)
            return COMPILATION_ERROR
        }

        if (outputName == null) {
            messageCollector.report(ERROR, "IR: Specify output name via -ir-output-name", null)
            return COMPILATION_ERROR
        }

        val moduleName = arguments.irModuleName ?: outputName
        val outputDir = File(outputDirPath)

        val compilerImpl: K2JsCompilerImplBase
        if (arguments.wasm) {
            compilerImpl = K2WasmCompilerImpl(
                arguments = arguments,
                configuration = configuration,
                moduleName = moduleName,
                outputName = outputName,
                outputDir = outputDir,
                messageCollector = messageCollector,
                performanceManager = performanceManager,
            )
        } else {
            compilerImpl = K2JsCompilerImpl(
                arguments = arguments,
                configuration = configuration,
                moduleName = moduleName,
                outputName = outputName,
                outputDir = outputDir,
                messageCollector = messageCollector,
                performanceManager = performanceManager,
            )
        }

        compilerImpl.checkTargetArguments()?.let { return it }

        val pluginLoadResult = loadPlugins(paths, arguments, configuration, rootDisposable)
        if (pluginLoadResult != OK) return pluginLoadResult

        CommonWebConfigurationUpdater.initializeCommonConfiguration(compilerImpl.configuration, arguments)
        val libraries = configuration.libraries
        val friendLibraries = configuration.friendLibraries

        val targetEnvironment = compilerImpl.tryInitializeCompiler(rootDisposable) ?: return COMPILATION_ERROR

        val zipAccessor = DisposableZipFileSystemAccessor(64)
        Disposer.register(rootDisposable, zipAccessor)
        targetEnvironment.configuration.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)

        val sourcesFiles = targetEnvironment.getSourceFiles()

        if (!checkKotlinPackageUsageForPsi(targetEnvironment.configuration, sourcesFiles)) return COMPILATION_ERROR

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        // Produce KLIBs and get module (run analysis if main module is sources)
        val klibs = loadWebKlibsInProductionPipeline(configuration, configuration.platformChecker)
        runStandardLibrarySpecialCompatibilityChecks(klibs.all, isWasm = arguments.wasm, messageCollector)

        var sourceModule: ModulesStructure? = null
        val includes = configuration.includes
        if (includes == null) {
            val outputKlibPath =
                if (arguments.irProduceKlibFile) outputDir.resolve("$outputName.klib").normalize().absolutePath
                else outputDirPath
            sourceModule = produceSourceModule(targetEnvironment, klibs, arguments, outputKlibPath)

            if (configuration.get(CommonConfigurationKeys.USE_FIR) != true && !sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode)
                return OK
        }

        if (!arguments.irProduceJs) {
            performanceManager?.notifyPhaseFinished(PhaseType.TranslationToIr)
            return OK
        }

        val cacheDirectory = arguments.cacheDirectory
        val moduleKind = targetEnvironment.configuration.get(JSConfigurationKeys.MODULE_KIND)
        // TODO: Handle non-empty main call arguments
        val mainCallArguments = if (K2JsArgumentConstants.NO_CALL == arguments.main) null else emptyList<String>()

        messageCollector.report(INFO, "Produce executable: $outputDirPath")
        messageCollector.report(INFO, "Cache directory: $cacheDirectory")

        if (cacheDirectory != null) {
            val icCacheReadOnly = arguments.wasm && arguments.icCacheReadonly
            val cacheGuard = IncrementalCacheGuard(cacheDirectory, icCacheReadOnly)
            when (cacheGuard.acquire()) {
                IncrementalCacheGuard.AcquireStatus.CACHE_CLEARED -> {
                    messageCollector.report(INFO, "Cache guard file detected, cache directory '$cacheDirectory' cleared")
                }
                IncrementalCacheGuard.AcquireStatus.INVALID_CACHE -> {
                    messageCollector.report(
                        ERROR,
                        "Cache guard file detected in readonly mode, cache directory '$cacheDirectory' should be cleared"
                    )
                    return INTERNAL_ERROR
                }
                IncrementalCacheGuard.AcquireStatus.OK -> {
                }
            }

            val icCaches = prepareIcCaches(
                cacheDirectory = cacheDirectory,
                arguments = arguments,
                messageCollector = messageCollector,
                outputDir = outputDir,
                targetConfiguration = targetEnvironment.configuration,
                mainCallArguments = mainCallArguments,
                icCacheReadOnly = icCacheReadOnly,
            )

            cacheGuard.release()

            // We use one cache directory for both caches: JS AST and JS code.
            // This guard MUST be unlocked after a successful preparing icCaches (see prepareIcCaches()).
            // Do not use IncrementalCacheGuard::acquire() - it may drop an entire cache here, and
            // it breaks the logic from JsExecutableProducer(), therefore use IncrementalCacheGuard::tryAcquire() instead
            // TODO: One day, when we will lower IR and produce JS AST per module,
            //      think about using different directories for JS AST and JS code.
            cacheGuard.tryAcquire()
            val icCompileResult = compilerImpl.compileWithIC(icCaches, targetEnvironment.configuration, moduleKind)
            cacheGuard.release()
            return icCompileResult
        }

        val module = if (includes != null) {
            if (sourcesFiles.isNotEmpty()) {
                messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
            }
            val includesPath = File(includes).canonicalPath
            val mainLibPath = libraries.find { File(it).canonicalPath == includesPath }
                ?: error("No library with name $includes ($includesPath) found")
            val kLib = MainModule.Klib(mainLibPath)
            ModulesStructure(
                project = targetEnvironment.project,
                mainModule = kLib,
                compilerConfiguration = targetEnvironment.configuration,
                klibs = klibs
            )
        } else {
            sourceModule!!
        }

        return compilerImpl.compileNoIC(mainCallArguments, module, moduleKind)
    }

    private fun produceSourceModule(
        environmentForJS: KotlinCoreEnvironment,
        klibs: LoadedKlibs,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String,
    ): ModulesStructure {
        val performanceManager = environmentForJS.configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
        performanceManager?.notifyCurrentPhaseFinishedIfNeeded()
        lateinit var sourceModule: ModulesStructure
        performanceManager.tryMeasurePhaseTime(PhaseType.Analysis) {
            do {
                val analyzerFacade = when (arguments.wasm) {
                    true -> TopDownAnalyzerFacadeForWasm.facadeFor(environmentForJS.configuration.get(WasmConfigurationKeys.WASM_TARGET))
                    else -> TopDownAnalyzerFacadeForJSIR
                }
                sourceModule = prepareAnalyzedSourceModule(
                    environmentForJS.project,
                    environmentForJS.getSourceFiles(),
                    environmentForJS.configuration,
                    klibs,
                    AnalyzerWithCompilerReport(environmentForJS.configuration),
                    analyzerFacade = analyzerFacade
                )
                val result = sourceModule.jsFrontEndResult.jsAnalysisResult
                if (result is JsAnalysisResult.RetryWithAdditionalRoots) {
                    environmentForJS.addKotlinSourceRoots(result.additionalKotlinRoots)
                }
            } while (result is JsAnalysisResult.RetryWithAdditionalRoots)
        }

        if (sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode && (arguments.irProduceKlibDir || arguments.irProduceKlibFile)) {
            val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
            val icData = environmentForJS.configuration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()

            val (moduleFragment, irPluginContext) = generateIrForKlibSerialization(
                project = environmentForJS.project,
                files = moduleSourceFiles,
                configuration = environmentForJS.configuration,
                analysisResult = sourceModule.jsFrontEndResult.jsAnalysisResult,
                klibs = sourceModule.klibs,
                icData = icData,
                irFactory = IrFactoryImpl,
            ) {
                sourceModule.getModuleDescriptor(it)
            }

            val messageCollector = environmentForJS.configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter(messageCollector)
            generateKLib(
                sourceModule,
                outputKlibPath,
                nopack = arguments.irProduceKlibDir,
                jsOutputName = arguments.irPerModuleOutputName,
                icData = icData,
                moduleFragment = moduleFragment,
                irBuiltIns = irPluginContext.irBuiltIns,
                diagnosticReporter = diagnosticsReporter,
                builtInsPlatform = if (arguments.wasm) BuiltInsPlatform.WASM else BuiltInsPlatform.JS,
                wasmTarget = if (!arguments.wasm) null else arguments.wasmTarget?.let(WasmTarget::fromName)
            )

            reportCollectedDiagnostics(environmentForJS.configuration, diagnosticsReporter, messageCollector)
            if (diagnosticsReporter.hasErrors) {
                throw CompilationErrorException()
            }
        }
        return sourceModule
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services,
    ) {
        CommonWebConfigurationUpdater.setupPlatformSpecificArgumentsAndServices(configuration, arguments, services)
    }

    override fun executableScriptFileName(): String = "kotlinc-js"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return MetadataVersion(*versionArray)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JSCompilerArguments) {}

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JSCompiler(), args)
        }
    }
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
    messageCollector: MessageCollector,
): RuntimeDiagnostic? = when (value?.lowercase()) {
    RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        messageCollector.report(STRONG_WARNING, "Unknown runtime diagnostic '$value'")
        null
    }
}

fun loadPluginsForTests(configuration: CompilerConfiguration, parentDisposable: Disposable): ExitCode {
    var pluginClasspath: Iterable<String> = emptyList()
    val kotlinPaths = PathUtil.kotlinPathsForCompiler
    val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
    val (jars, _) =
        PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }.partition { it.exists() }
    pluginClasspath = jars.map { it.canonicalPath } + pluginClasspath

    return PluginCliParser.loadPluginsSafe(pluginClasspath, listOf(), listOf(), configuration, parentDisposable)
}

