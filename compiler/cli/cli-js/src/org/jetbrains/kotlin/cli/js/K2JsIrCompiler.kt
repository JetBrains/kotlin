/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.js.klib.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.getModuleNameForSource
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.join
import org.jetbrains.kotlin.wasm.config.WasmConfigurationKeys
import java.io.File
import java.io.IOException

private val K2JSCompilerArguments.granularity: JsGenerationGranularity
    get() = when {
        this.irPerFile -> JsGenerationGranularity.PER_FILE
        this.irPerModule -> JsGenerationGranularity.PER_MODULE
        else -> JsGenerationGranularity.WHOLE_PROGRAM
    }

private val K2JSCompilerArguments.dtsStrategy: TsCompilationStrategy
    get() = when {
        !this.generateDts -> TsCompilationStrategy.NONE
        this.irPerFile -> TsCompilationStrategy.EACH_FILE
        else -> TsCompilationStrategy.MERGED
    }

private class DisposableZipFileSystemAccessor private constructor(
    private val zipAccessor: ZipFileSystemCacheableAccessor
) : Disposable, ZipFileSystemAccessor by zipAccessor {
    constructor(cacheLimit: Int) : this(ZipFileSystemCacheableAccessor(cacheLimit))

    override fun dispose() {
        zipAccessor.reset()
    }
}

class K2JsIrCompiler : CLICompiler<K2JSCompilerArguments>() {
    class K2JsIrCompilerPerformanceManager : CommonCompilerPerformanceManager("Kotlin to JS (IR) Compiler")

    override val defaultPerformanceManager: CommonCompilerPerformanceManager = K2JsIrCompilerPerformanceManager()

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    private class Ir2JsTransformer(
        val arguments: K2JSCompilerArguments,
        val module: ModulesStructure,
        val phaseConfig: PhaseConfig,
        val messageCollector: MessageCollector,
        val mainCallArguments: List<String>?
    ) {
        private val performanceManager = module.compilerConfiguration[CLIConfigurationKeys.PERF_MANAGER]

        private fun lowerIr(): LoweredIr {
            return compile(
                mainCallArguments,
                module,
                phaseConfig,
                IrFactoryImplForJsIC(WholeWorldStageController()),
                keep = arguments.irKeep?.split(",")
                    ?.filterNot { it.isEmpty() }
                    ?.toSet()
                    ?: emptySet(),
                dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irDceRuntimeDiagnostic,
                    messageCollector
                ),
                safeExternalBoolean = arguments.irSafeExternalBoolean,
                safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irSafeExternalBooleanDiagnostic,
                    messageCollector
                ),
                granularity = arguments.granularity,
            )
        }

        private fun makeJsCodeGenerator(): JsCodeGenerator {
            val ir = lowerIr()
            val transformer = IrModuleToJsTransformer(ir.context, ir.moduleFragmentToUniqueName, mainCallArguments != null)

            val mode = TranslationMode.fromFlags(arguments.irDce, arguments.granularity, arguments.irMinimizedMemberNames)
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


    private val K2JSCompilerArguments.targetVersion: EcmaVersion?
        get() {
            val targetString = target
            return when {
                targetString != null -> EcmaVersion.entries.firstOrNull { it.name == targetString }
                else -> EcmaVersion.defaultVersion()
            }
        }

    override fun doExecute(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]

        val targetVersion = arguments.targetVersion?.also {
            configuration.put(JSConfigurationKeys.TARGET, it)
        }

        if (targetVersion == null) {
            messageCollector.report(ERROR, "Unsupported ECMA version: ${arguments.target}")
            return COMPILATION_ERROR
        }

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
        if (pluginLoadResult != OK) return pluginLoadResult

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

        val libraries: List<String> = configureLibraries(arguments.libraries) + listOfNotNull(arguments.includes)
        val friendLibraries: List<String> = configureLibraries(arguments.friendModules)

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, libraries)
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)
        configuration.put(WasmConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.wasmEnableAsserts)
        configuration.put(WasmConfigurationKeys.WASM_GENERATE_WAT, arguments.wasmGenerateWat)
        configuration.put(WasmConfigurationKeys.WASM_USE_TRAPS_INSTEAD_OF_EXCEPTIONS, arguments.wasmUseTrapsInsteadOfExceptions)
        configuration.put(WasmConfigurationKeys.WASM_USE_NEW_EXCEPTION_PROPOSAL, arguments.wasmUseNewExceptionProposal)
        configuration.putIfNotNull(WasmConfigurationKeys.WASM_TARGET, arguments.wasmTarget?.let(WasmTarget::fromName))

        configuration.put(JSConfigurationKeys.OPTIMIZE_GENERATED_JS, arguments.optimizeGeneratedJs)

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        val hmppCliModuleStructure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg), hmppCliModuleStructure?.getModuleNameForSource(arg))
        }

        arguments.relativePathBases?.let {
            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, it.toList())
        }

        configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, arguments.normalizeAbsolutePath)
        configuration.put(CommonConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS, arguments.enableSignatureClashChecks)

        // ----

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        val projectJs = environmentForJS.project
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

        arguments.platformArgumentsProviderJsExpression?.let {
            configurationJs.put(JSConfigurationKeys.DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS, it)
        }

        val zipAccessor = DisposableZipFileSystemAccessor(64)
        Disposer.register(rootDisposable, zipAccessor)
        configurationJs.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)

        if (!checkKotlinPackageUsageForPsi(environmentForJS.configuration, sourcesFiles)) return COMPILATION_ERROR

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

        if (messageCollector.hasErrors()) {
            return COMPILATION_ERROR
        }

        if (sourcesFiles.isEmpty() && (!incrementalCompilationIsEnabledForJs(arguments)) && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return COMPILATION_ERROR
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        val moduleName = arguments.irModuleName ?: outputName
        configurationJs.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        val outputDir = File(outputDirPath)

        try {
            configurationJs.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return COMPILATION_ERROR
        }

        performanceManager?.notifyCompilerInitialized(
            sourcesFiles.size, environmentForJS.countLinesOfCode(sourcesFiles), "$moduleName-$moduleKind"
        )

        // TODO: Handle non-empty main call arguments
        val mainCallArguments = if (K2JsArgumentConstants.NO_CALL == arguments.main) null else emptyList<String>()

        val icCaches = prepareIcCaches(
            arguments = arguments,
            messageCollector = messageCollector,
            outputDir = outputDir,
            libraries = libraries,
            friendLibraries = friendLibraries,
            configurationJs = configurationJs,
            mainCallArguments = mainCallArguments
        )

        // Run analysis if main module is sources
        var sourceModule: ModulesStructure? = null
        val includes = arguments.includes
        if (includes == null) {
            val outputKlibPath =
                if (arguments.irProduceKlibFile) outputDir.resolve("$outputName.klib").normalize().absolutePath
                else outputDirPath
            sourceModule = produceSourceModule(configuration, environmentForJS, libraries, friendLibraries, arguments, outputKlibPath)

            if (configuration.get(CommonConfigurationKeys.USE_FIR) != true && !sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode)
                return OK
        }

        if (!arguments.irProduceJs) {
            performanceManager?.notifyIRTranslationFinished()
            return OK
        }

        messageCollector.report(INFO, "Produce executable: $outputDirPath")
        messageCollector.report(INFO, "Cache directory: ${arguments.cacheDirectory}")

        if (icCaches != null) {
            val beforeIc2Js = System.currentTimeMillis()

            // We use one cache directory for both caches: JS AST and JS code.
            // This guard MUST be unlocked after a successful preparing icCaches (see prepareIcCaches()).
            // Do not use IncrementalCacheGuard::acquire() - it may drop an entire cache here, and
            // it breaks the logic from JsExecutableProducer(), therefore use IncrementalCacheGuard::tryAcquire() instead
            // TODO: One day, when we will lower IR and produce JS AST per module,
            //      think about using different directories for JS AST and JS code.
            icCaches.cacheGuard.tryAcquire()

            val jsExecutableProducer = JsExecutableProducer(
                mainModuleName = moduleName,
                moduleKind = moduleKind,
                sourceMapsInfo = SourceMapsInfo.from(configurationJs),
                caches = icCaches.artifacts,
                relativeRequirePath = true
            )

            val (outputs, rebuiltModules) = jsExecutableProducer.buildExecutable(arguments.granularity, outJsProgram = false)
            outputs.writeAll(outputDir, outputName, arguments.dtsStrategy, moduleName, moduleKind)

            icCaches.cacheGuard.release()

            messageCollector.report(INFO, "Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
            for ((event, duration) in jsExecutableProducer.getStopwatchLaps()) {
                messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
            }

            for (module in rebuiltModules) {
                messageCollector.report(INFO, "IC module builder rebuilt JS for module [${File(module).name}]")
            }

            performanceManager?.notifyIRTranslationFinished()
            return OK
        }

        val phaseConfig = createPhaseConfig(jsPhases, arguments, messageCollector)

        val module = if (includes != null) {
            if (sourcesFiles.isNotEmpty()) {
                messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
            }
            val includesPath = File(includes).canonicalPath
            val mainLibPath = libraries.find { File(it).canonicalPath == includesPath }
                ?: error("No library with name $includes ($includesPath) found")
            val kLib = MainModule.Klib(mainLibPath)
            ModulesStructure(
                projectJs,
                kLib,
                configurationJs,
                libraries,
                friendLibraries
            )
        } else {
            sourceModule!!
        }

        if (arguments.wasm) {
            val generateDts = configuration.getBoolean(JSConfigurationKeys.GENERATE_DTS)
            val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)

            val irFactory = IrFactoryImplForWasmIC(WholeWorldStageController())

            val (allModules, backendContext, typeScriptFragment) = compileToLoweredIr(
                depsDescriptors = module,
                phaseConfig = createPhaseConfig(wasmPhases, arguments, messageCollector),
                irFactory = irFactory,
                exportedDeclarations = setOf(FqName("main")),
                generateTypeScriptFragment = generateDts,
                propertyLazyInitialization = arguments.irPropertyLazyInitialization,
            )

            performanceManager?.notifyIRGenerationStarted()
            val dceDumpNameCache = DceDumpNameCache()
            if (arguments.irDce) {
                eliminateDeadDeclarations(allModules, backendContext, dceDumpNameCache)
            }

            dumpDeclarationIrSizesIfNeed(arguments.irDceDumpDeclarationIrSizesToFile, allModules, dceDumpNameCache)

            val res = compileWasm(
                allModules = allModules,
                backendContext = backendContext,
                typeScriptFragment = typeScriptFragment,
                baseFileName = outputName,
                idSignatureRetriever = irFactory,
                emitNameSection = arguments.wasmDebug,
                allowIncompleteImplementations = arguments.irDce,
                    generateWat = configuration.get(WasmConfigurationKeys.WASM_GENERATE_WAT, false),
                generateSourceMaps = generateSourceMaps,
            )
            performanceManager?.notifyIRGenerationFinished()
            performanceManager?.notifyGenerationFinished()

            writeCompilationResult(
                result = res,
                dir = outputDir,
                fileNameBase = outputName,
            )

            return OK
        } else {
            if (arguments.irDceDumpReachabilityInfoToFile != null) {
                messageCollector.report(STRONG_WARNING, "Dumping the reachability info to file is supported only for Kotlin/Wasm.")
            }
            if (arguments.irDceDumpDeclarationIrSizesToFile != null) {
                messageCollector.report(STRONG_WARNING, "Dumping the size of declarations to file is supported only for Kotlin/Wasm.")
            }
        }

        val start = System.currentTimeMillis()

        try {
            val ir2JsTransformer = Ir2JsTransformer(arguments, module, phaseConfig, messageCollector, mainCallArguments)
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

    private fun produceSourceModule(
        configuration: CompilerConfiguration,
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String,
    ): ModulesStructure {
        val performanceManager = configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager?.notifyAnalysisStarted()

        val sourceModule = if (configuration.get(CommonConfigurationKeys.USE_FIR) == true) {
            processSourceModuleWithK2(environmentForJS, libraries, friendLibraries, arguments, outputKlibPath)
        } else {
            processSourceModuleWithK1(environmentForJS, libraries, friendLibraries, arguments, outputKlibPath)
        }

        return sourceModule
    }

    private fun processSourceModuleWithK1(
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure {
        val performanceManager = environmentForJS.configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        lateinit var sourceModule: ModulesStructure
        do {
            val analyzerFacade = when (arguments.wasm) {
                true -> TopDownAnalyzerFacadeForWasm.facadeFor(environmentForJS.configuration.get(WasmConfigurationKeys.WASM_TARGET))
                else -> TopDownAnalyzerFacadeForJSIR
            }
            sourceModule = prepareAnalyzedSourceModule(
                environmentForJS.project,
                environmentForJS.getSourceFiles(),
                environmentForJS.configuration,
                libraries,
                friendLibraries,
                AnalyzerWithCompilerReport(environmentForJS.configuration),
                analyzerFacade = analyzerFacade
            )
            val result = sourceModule.jsFrontEndResult.jsAnalysisResult
            if (result is JsAnalysisResult.RetryWithAdditionalRoots) {
                environmentForJS.addKotlinSourceRoots(result.additionalKotlinRoots)
            }
        } while (result is JsAnalysisResult.RetryWithAdditionalRoots)
        performanceManager?.notifyAnalysisFinished()

        if (sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode && (arguments.irProduceKlibDir || arguments.irProduceKlibFile)) {
            val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
            val icData = environmentForJS.configuration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()

            val (moduleFragment, _) = generateIrForKlibSerialization(
                environmentForJS.project,
                moduleSourceFiles,
                environmentForJS.configuration,
                sourceModule.jsFrontEndResult.jsAnalysisResult,
                sourceModule.allDependencies,
                icData,
                IrFactoryImpl,
                verifySignatures = true
            ) {
                sourceModule.getModuleDescriptor(it)
            }

            val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
            generateKLib(
                sourceModule,
                outputKlibPath,
                nopack = arguments.irProduceKlibDir,
                jsOutputName = arguments.irPerModuleOutputName,
                icData = icData,
                moduleFragment = moduleFragment,
                diagnosticReporter = diagnosticsReporter,
                builtInsPlatform = if (arguments.wasm) BuiltInsPlatform.WASM else BuiltInsPlatform.JS,
                wasmTarget = if (!arguments.wasm) null else arguments.wasmTarget?.let(WasmTarget::fromName)
            )

            val messageCollector = environmentForJS.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            reportCollectedDiagnostics(environmentForJS.configuration, diagnosticsReporter, messageCollector)
            if (diagnosticsReporter.hasErrors) {
                throw CompilationErrorException()
            }
        }
        return sourceModule
    }

    private fun processSourceModuleWithK2(
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure {
        val configuration = environmentForJS.configuration
        val performanceManager = configuration.get(CLIConfigurationKeys.PERF_MANAGER)
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val mainModule = MainModule.SourceFiles(environmentForJS.getSourceFiles())
        val moduleStructure = ModulesStructure(environmentForJS.project, mainModule, configuration, libraries, friendLibraries)

        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING

        val analyzedOutput = if (
            configuration.getBoolean(CommonConfigurationKeys.USE_FIR) && configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
        ) {
            val groupedSources = collectSources(configuration, environmentForJS.project, messageCollector)

            compileModulesToAnalyzedFirWithLightTree(
                moduleStructure = moduleStructure,
                groupedSources = groupedSources,
                // TODO: Only pass groupedSources, because
                //  we will need to have them separated again
                //  in createSessionsForLegacyMppProject anyway
                ktSourceFiles = groupedSources.commonSources + groupedSources.platformSources,
                libraries = libraries,
                friendLibraries = friendLibraries,
                diagnosticsReporter = diagnosticsReporter,
                incrementalDataProvider = configuration[JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER],
                lookupTracker = lookupTracker,
                useWasmPlatform = arguments.wasm,
            )
        } else {
            compileModuleToAnalyzedFirWithPsi(
                moduleStructure = moduleStructure,
                ktFiles = environmentForJS.getSourceFiles(),
                libraries = libraries,
                friendLibraries = friendLibraries,
                diagnosticsReporter = diagnosticsReporter,
                incrementalDataProvider = configuration[JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER],
                lookupTracker = lookupTracker,
                useWasmPlatform = arguments.wasm,
            )
        }

        performanceManager?.notifyAnalysisFinished()
        if (analyzedOutput.reportCompilationErrors(moduleStructure, diagnosticsReporter, messageCollector)) {
            throw CompilationErrorException()
        }

        // FIR2IR
        performanceManager?.notifyIRTranslationStarted()
        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, true)
        if (diagnosticsReporter.hasErrors) {
            throw CompilationErrorException("Compilation failed: there were some diagnostics during fir2ir")
        }

        if (configuration.getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)) {
            // TODO: During checking the next round, fir serializer may throw an exception, e.g.
            //      during annotation serialization when it cannot find the removed constant
            //      (see ConstantValueUtils.kt:convertToConstantValues())
            //  This happens because we check the next round before compilation errors.
            //  Test reproducer:  testFileWithConstantRemoved
            //  Issue: https://youtrack.jetbrains.com/issue/KT-58824/
            val shouldGoToNextIcRound = shouldGoToNextIcRound(moduleStructure.compilerConfiguration) {
                Fir2KlibMetadataSerializer(
                    moduleStructure.compilerConfiguration,
                    analyzedOutput.output,
                    fir2IrActualizedResult,
                    exportKDoc = false,
                    produceHeaderKlib = false,
                )
            }
            if (shouldGoToNextIcRound) {
                throw IncrementalNextRoundException()
            }
        }

        // Serialize klib
        if (arguments.irProduceKlibDir || arguments.irProduceKlibFile) {
            serializeFirKlib(
                moduleStructure = moduleStructure,
                firOutputs = analyzedOutput.output,
                fir2IrActualizedResult = fir2IrActualizedResult,
                outputKlibPath = outputKlibPath,
                nopack = arguments.irProduceKlibDir,
                messageCollector = messageCollector,
                diagnosticsReporter = diagnosticsReporter,
                jsOutputName = arguments.irPerModuleOutputName,
                useWasmPlatform = arguments.wasm,
                wasmTarget = arguments.wasmTarget?.let(WasmTarget::fromName)
            )

            reportCollectedDiagnostics(moduleStructure.compilerConfiguration, diagnosticsReporter, messageCollector)
            if (diagnosticsReporter.hasErrors) {
                throw CompilationErrorException()
            }
        }

        return moduleStructure
    }

    class IcCachesArtifacts(val artifacts: List<ModuleArtifact>, val cacheGuard: IncrementalCacheGuard)

    private fun prepareIcCaches(
        arguments: K2JSCompilerArguments,
        messageCollector: MessageCollector,
        outputDir: File,
        libraries: List<String>,
        friendLibraries: List<String>,
        configurationJs: CompilerConfiguration,
        mainCallArguments: List<String>?
    ): IcCachesArtifacts? {
        val cacheDirectory = arguments.cacheDirectory

        // TODO: Use JS IR IC infrastructure for WASM?
        if (!arguments.wasm && cacheDirectory != null) {
            val cacheGuard = IncrementalCacheGuard(cacheDirectory).also {
                if (it.acquire() == IncrementalCacheGuard.AcquireStatus.CACHE_CLEARED) {
                    messageCollector.report(INFO, "Cache guard file detected, cache directory '$cacheDirectory' cleared")
                }
            }

            messageCollector.report(INFO, "")
            messageCollector.report(INFO, "Building cache:")
            messageCollector.report(INFO, "to: $outputDir")
            messageCollector.report(INFO, "cache directory: $cacheDirectory")
            messageCollector.report(INFO, libraries.toString())

            val start = System.currentTimeMillis()

            val cacheUpdater = CacheUpdater(
                mainModule = arguments.includes!!,
                allModules = libraries,
                mainModuleFriends = friendLibraries,
                cacheDir = cacheDirectory,
                compilerConfiguration = configurationJs,
                irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
                compilerInterfaceFactory = { mainModule, cfg ->
                    JsIrCompilerWithIC(
                        mainModule,
                        mainCallArguments,
                        cfg,
                        arguments.granularity,
                        PhaseConfig(jsPhases),
                    )
                }
            )

            val artifacts = cacheUpdater.actualizeCaches()
            cacheGuard.release()

            messageCollector.report(INFO, "IC rebuilt overall time: ${System.currentTimeMillis() - start}ms")
            for ((event, duration) in cacheUpdater.getStopwatchLastLaps()) {
                messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
            }

            var libIndex = 0
            for ((libFile, srcFiles) in cacheUpdater.getDirtyFileLastStats()) {
                val singleState = srcFiles.values.firstOrNull()?.singleOrNull()?.let { singleState ->
                    singleState.takeIf { srcFiles.values.all { it.singleOrNull() == singleState } }
                }

                val (msg, showFiles) = when {
                    singleState == DirtyFileState.NON_MODIFIED_IR -> continue
                    singleState == DirtyFileState.REMOVED_FILE -> "removed" to emptyMap()
                    singleState == DirtyFileState.ADDED_FILE -> "built clean" to emptyMap()
                    srcFiles.values.any { it.singleOrNull() == DirtyFileState.NON_MODIFIED_IR } -> "partially rebuilt" to srcFiles
                    else -> "fully rebuilt" to srcFiles
                }
                messageCollector.report(INFO, "${++libIndex}) module [${File(libFile.path).name}] was $msg")
                var fileIndex = 0
                for ((srcFile, stat) in showFiles) {
                    val filteredStats = stat.filter { it != DirtyFileState.NON_MODIFIED_IR }
                    val statStr = filteredStats.takeIf { it.isNotEmpty() }?.joinToString { it.str } ?: continue
                    // Use index, because MessageCollector ignores already reported messages
                    messageCollector.report(INFO, "  $libIndex.${++fileIndex}) file [${File(srcFile.path).name}]: ($statStr)")
                }
            }

            return IcCachesArtifacts(artifacts, cacheGuard)
        }
        return null
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        if (arguments.sourceMap) {
            configuration.put(JSConfigurationKeys.SOURCE_MAP, true)
            if (arguments.sourceMapPrefix != null) {
                configuration.put(JSConfigurationKeys.SOURCE_MAP_PREFIX, arguments.sourceMapPrefix!!)
            }

            var sourceMapSourceRoots = arguments.sourceMapBaseDirs
            if (sourceMapSourceRoots == null && StringUtil.isNotEmpty(arguments.sourceMapPrefix)) {
                sourceMapSourceRoots = K2JSCompiler.calculateSourceMapSourceRoot(messageCollector, arguments)
            }

            if (sourceMapSourceRoots != null) {
                val sourceMapSourceRootList = StringUtil.split(sourceMapSourceRoots, File.pathSeparator)
                configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceMapSourceRootList)
            }

        } else {
            if (arguments.sourceMapPrefix != null) {
                messageCollector.report(WARNING, "source-map-prefix argument has no effect without source map", null)
            }
            if (arguments.sourceMapBaseDirs != null) {
                messageCollector.report(WARNING, "source-map-source-root argument has no effect without source map", null)
            }
        }

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, arguments.typedArrays)

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.friendModulesDisabled)

        configuration.put(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT, arguments.strictImplicitExportType)


        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        if (arguments.wasm) {
            // K/Wasm support ES modules only.
            configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.ES)
        }

        configuration.putIfNotNull(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER, services[IncrementalDataProvider::class.java])
        configuration.putIfNotNull(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, services[IncrementalResultsConsumer::class.java])
        configuration.putIfNotNull(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER, services[IncrementalNextRoundChecker::class.java])
        configuration.putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, services[LookupTracker::class.java])
        configuration.putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, services[ExpectActualTracker::class.java])

        val errorTolerancePolicy = arguments.errorTolerancePolicy?.let { ErrorTolerancePolicy.resolvePolicy(it) }
        configuration.putIfNotNull(JSConfigurationKeys.ERROR_TOLERANCE_POLICY, errorTolerancePolicy)

        if (errorTolerancePolicy?.allowErrors == true) {
            configuration.put(JSConfigurationKeys.DEVELOPER_MODE, true)
        }

        val sourceMapEmbedContentString = arguments.sourceMapEmbedSources
        var sourceMapContentEmbedding: SourceMapSourceEmbedding? = if (sourceMapEmbedContentString != null)
            sourceMapContentEmbeddingMap[sourceMapEmbedContentString]
        else
            SourceMapSourceEmbedding.INLINING
        if (sourceMapContentEmbedding == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map source embedding mode: $sourceMapEmbedContentString. Valid values are: ${sourceMapContentEmbeddingMap.keys.joinToString()}"
            )
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapContentEmbedding)

        if (!arguments.sourceMap && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null)
        }

        val sourceMapNamesPolicyString = arguments.sourceMapNamesPolicy
        var sourceMapNamesPolicy: SourceMapNamesPolicy? = if (sourceMapNamesPolicyString != null)
            sourceMapNamesPolicyMap[sourceMapNamesPolicyString]
        else
            SourceMapNamesPolicy.SIMPLE_NAMES
        if (sourceMapNamesPolicy == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map names policy: $sourceMapNamesPolicyString. Valid values are: ${sourceMapNamesPolicyMap.keys.joinToString()}"
            )
            sourceMapNamesPolicy = SourceMapNamesPolicy.SIMPLE_NAMES
        }
        configuration.put(JSConfigurationKeys.SOURCEMAP_NAMES_POLICY, sourceMapNamesPolicy)

        configuration.put(JSConfigurationKeys.PRINT_REACHABILITY_INFO, arguments.irDcePrintReachabilityInfo)
        configuration.put(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
        configuration.putIfNotNull(JSConfigurationKeys.DUMP_REACHABILITY_INFO_TO_FILE, arguments.irDceDumpReachabilityInfoToFile)

        configuration.setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage =
            /* no PL when producing KLIB */ arguments.includes != null,
            onWarning = { messageCollector.report(WARNING, it) },
            onError = { messageCollector.report(ERROR, it) }
        )
    }

    override fun executableScriptFileName(): String {
        TODO("Provide a proper way to run the compiler with IR BE")
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return KlibMetadataVersion(*versionArray)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JSCompilerArguments) {}

    companion object {
        private val moduleKindMap = mapOf(
            K2JsArgumentConstants.MODULE_PLAIN to ModuleKind.PLAIN,
            K2JsArgumentConstants.MODULE_COMMONJS to ModuleKind.COMMON_JS,
            K2JsArgumentConstants.MODULE_AMD to ModuleKind.AMD,
            K2JsArgumentConstants.MODULE_UMD to ModuleKind.UMD,
            K2JsArgumentConstants.MODULE_ES to ModuleKind.ES,
        )
        private val sourceMapContentEmbeddingMap = mapOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS to SourceMapSourceEmbedding.ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER to SourceMapSourceEmbedding.NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING to SourceMapSourceEmbedding.INLINING
        )

        private val sourceMapNamesPolicyMap = mapOf(
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_NO to SourceMapNamesPolicy.NO,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES to SourceMapNamesPolicy.SIMPLE_NAMES,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_FQ_NAMES to SourceMapNamesPolicy.FULLY_QUALIFIED_NAMES
        )

        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JsIrCompiler(), args)
        }

        private fun reportCompiledSourcesList(messageCollector: MessageCollector, sourceFiles: List<KtFile>) {
            val fileNames = sourceFiles.map { file ->
                val virtualFile = file.virtualFile
                if (virtualFile != null) {
                    MessageUtil.virtualFileToPath(virtualFile)
                } else {
                    file.name + " (no virtual file)"
                }
            }
            messageCollector.report(LOGGING, "Compiling source files: " + join(fileNames, ", "), null)
        }

        private fun configureLibraries(libraryString: String?): List<String> =
            libraryString?.splitByPathSeparator() ?: emptyList()

        private fun String.splitByPathSeparator(): List<String> {
            return this.split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
                .filterNot { it.isEmpty() }
        }
    }
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
    messageCollector: MessageCollector
): RuntimeDiagnostic? = when (value?.lowercase()) {
    RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        messageCollector.report(STRONG_WARNING, "Unknown runtime diagnostic '$value'")
        null
    }
}

fun loadPluginsForTests(configuration: CompilerConfiguration): ExitCode {
    var pluginClasspath: Iterable<String> = emptyList()
    val kotlinPaths = PathUtil.kotlinPathsForCompiler
    val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
    val (jars, _) =
        PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }.partition { it.exists() }
    pluginClasspath = jars.map { it.canonicalPath } + pluginClasspath

    return PluginCliParser.loadPluginsSafe(pluginClasspath, listOf(), listOf(), configuration)
}
