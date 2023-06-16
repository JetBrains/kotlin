/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.wasm

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2WasmCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.common.web.SourceMapsUtils.calculateSourceMapSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.wasm.klib.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.getModuleNameForSource
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.linkage.partial.setupPartialLinkageConfig
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemCacheableAccessor
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.join
import java.io.File
import java.io.IOException

private class DisposableZipFileSystemAccessor private constructor(
    private val zipAccessor: ZipFileSystemCacheableAccessor
) : Disposable, ZipFileSystemAccessor by zipAccessor {
    constructor(cacheLimit: Int) : this(ZipFileSystemCacheableAccessor(cacheLimit))

    override fun dispose() {
        zipAccessor.reset()
    }
}

class K2WasmCompiler : CLICompiler<K2WasmCompilerArguments>() {
    override val defaultPerformanceManager: CommonCompilerPerformanceManager =
        object : CommonCompilerPerformanceManager("Kotlin to Wasm Compiler") {}

    override fun createArguments(): K2WasmCompilerArguments {
        return K2WasmCompilerArguments()
    }

    public override fun doExecute(
        arguments: K2WasmCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
        if (pluginLoadResult != OK) return pluginLoadResult

        if (arguments.script) {
            messageCollector.report(ERROR, "K/Wasm does not support Kotlin script (*.kts) files")
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
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.enableArrayRangeChecks)
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.enableAsserts)
        configuration.put(JSConfigurationKeys.WASM_GENERATE_WAT, arguments.generateWat)

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

        val environmentForWasm =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.WASM_CONFIG_FILES)
        val projectWasm = environmentForWasm.project
        val configurationWasm = environmentForWasm.configuration
        val sourcesFiles = environmentForWasm.getSourceFiles()

        configurationWasm.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configurationWasm.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)
        configurationWasm.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, arguments.propertyLazyInitialization)

        val zipAccessor = DisposableZipFileSystemAccessor(64)
        Disposer.register(rootDisposable, zipAccessor)
        configurationWasm.put(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR, zipAccessor)

        if (!checkKotlinPackageUsageForPsi(environmentForWasm.configuration, sourcesFiles)) return COMPILATION_ERROR

        val outputDirPath = arguments.outputDir
        val moduleName = arguments.moduleName
        if (outputDirPath == null) {
            messageCollector.report(ERROR, "Specify output dir via -ir-output-dir", null)
            return COMPILATION_ERROR
        }

        if (moduleName == null) {
            messageCollector.report(ERROR, "Specify output name via -ir-output-name", null)
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

        configurationWasm.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        val outputDir = File(outputDirPath)

        try {
            configurationWasm.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return COMPILATION_ERROR
        }

        // Run analysis if main module is sources
        var sourceModule: ModulesStructure? = null
        val includes = arguments.includes
        if (includes == null) {
            val outputKlibPath =
                if (arguments.produceKlibFile) outputDir.resolve("$moduleName.klib").normalize().absolutePath
                else outputDirPath
            if (configuration.get(CommonConfigurationKeys.USE_FIR) == true) {
                sourceModule = processSourceModuleWithK2(environmentForWasm, libraries, friendLibraries, arguments, outputKlibPath)
            } else {
                sourceModule = processSourceModule(environmentForWasm, libraries, friendLibraries, arguments, outputKlibPath)

                if (!sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode)
                    return OK
            }

        }

        if (arguments.produceWasm) {
            messageCollector.report(INFO, "Produce executable: $outputDirPath")
            messageCollector.report(INFO, "Cache directory: ${arguments.cacheDirectory}")

            val module = if (includes != null) {
                if (sourcesFiles.isNotEmpty()) {
                    messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
                }
                val includesPath = File(includes).canonicalPath
                val mainLibPath = libraries.find { File(it).canonicalPath == includesPath }
                    ?: error("No library with name $includes ($includesPath) found")
                val kLib = MainModule.Klib(mainLibPath)
                ModulesStructure(
                    projectWasm,
                    kLib,
                    configurationWasm,
                    libraries,
                    friendLibraries
                )
            } else {
                sourceModule!!
            }

            val (allModules, backendContext) = compileToLoweredIr(
                depsDescriptors = module,
                phaseConfig = PhaseConfig(wasmPhases),
                irFactory = IrFactoryImpl,
                exportedDeclarations = setOf(FqName("main")),
                propertyLazyInitialization = arguments.propertyLazyInitialization,
            )
            if (arguments.dce) {
                eliminateDeadDeclarations(allModules, backendContext)
            }

            dumpDeclarationIrSizesIfNeed(arguments.dumpDeclarationIrSizesToFile, allModules)

            val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)

            val res = compileWasm(
                allModules = allModules,
                backendContext = backendContext,
                baseFileName = moduleName,
                emitNameSection = arguments.debug,
                allowIncompleteImplementations = arguments.dce,
                generateWat = configuration.get(JSConfigurationKeys.WASM_GENERATE_WAT, false),
                generateSourceMaps = generateSourceMaps
            )

            writeCompilationResult(
                result = res,
                dir = outputDir,
                fileNameBase = moduleName,
            )

            return OK
        }

        return OK
    }

    private fun processSourceModule(
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2WasmCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure {
        lateinit var sourceModule: ModulesStructure
        do {
            sourceModule = prepareAnalyzedSourceModule(
                environmentForJS.project,
                environmentForJS.getSourceFiles(),
                environmentForJS.configuration,
                libraries,
                friendLibraries,
                AnalyzerWithCompilerReport(environmentForJS.configuration)
            )
            val result = sourceModule.jsFrontEndResult.jsAnalysisResult
            if (result is JsAnalysisResult.RetryWithAdditionalRoots) {
                environmentForJS.addKotlinSourceRoots(result.additionalKotlinRoots)
            }
        } while (result is JsAnalysisResult.RetryWithAdditionalRoots)

        if (sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode && (arguments.produceKlibDir || arguments.produceKlibFile)) {
            val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
            val icData = environmentForJS.configuration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
            val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

            val (moduleFragment, _) = generateIrForKlibSerialization(
                environmentForJS.project,
                moduleSourceFiles,
                environmentForJS.configuration,
                sourceModule.jsFrontEndResult.jsAnalysisResult,
                sourceModule.allDependencies,
                icData,
                expectDescriptorToSymbol,
                IrFactoryImpl,
                verifySignatures = true
            ) {
                sourceModule.getModuleDescriptor(it)
            }

            val metadataSerializer =
                KlibMetadataIncrementalSerializer(
                    environmentForJS.configuration,
                    sourceModule.project,
                    sourceModule.jsFrontEndResult.hasErrors
                )

            generateKLib(
                sourceModule,
                outputKlibPath,
                nopack = arguments.produceKlibDir,
                jsOutputName = arguments.moduleName,
                icData = icData,
                expectDescriptorToSymbol = expectDescriptorToSymbol,
                moduleFragment = moduleFragment,
                builtInsPlatform = BuiltInsPlatform.WASM
            ) { file ->
                metadataSerializer.serializeScope(file, sourceModule.jsFrontEndResult.bindingContext, moduleFragment.descriptor)
            }
        }
        return sourceModule
    }

    private fun processSourceModuleWithK2(
        environmentForWasm: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2WasmCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure {
        val configuration = environmentForWasm.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

        val mainModule = MainModule.SourceFiles(environmentForWasm.getSourceFiles())
        val moduleStructure = ModulesStructure(environmentForWasm.project, mainModule, configuration, libraries, friendLibraries)

        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: LookupTracker.DO_NOTHING

        val analyzedOutput = if (
            configuration.getBoolean(CommonConfigurationKeys.USE_FIR) && configuration.getBoolean(CommonConfigurationKeys.USE_LIGHT_TREE)
        ) {
            val groupedSources = collectSources(configuration, environmentForWasm.project, messageCollector)

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
            )
        } else {
            compileModuleToAnalyzedFirWithPsi(
                moduleStructure = moduleStructure,
                ktFiles = environmentForWasm.getSourceFiles(),
                libraries = libraries,
                friendLibraries = friendLibraries,
                diagnosticsReporter = diagnosticsReporter,
                incrementalDataProvider = configuration[JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER],
                lookupTracker = lookupTracker,
            )
        }

        // FIR2IR
        val fir2IrActualizedResult = transformFirToIr(moduleStructure, analyzedOutput.output, diagnosticsReporter)

        if (configuration.getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)) {
            // TODO: During checking the next round, fir serializer may throw an exception, e.g.
            //      during annotation serialization when it cannot find the removed constant
            //      (see ConstantValueUtils.kt:convertToConstantValues())
            //  This happens because we check the next round before compilation errors.
            //  Test reproducer:  testFileWithConstantRemoved
            //  Issue: https://youtrack.jetbrains.com/issue/KT-58824/
            if (shouldGoToNextIcRound(moduleStructure, analyzedOutput.output, fir2IrActualizedResult)) {
                throw IncrementalNextRoundException()
            }
        }

        if (analyzedOutput.reportCompilationErrors(moduleStructure, diagnosticsReporter, messageCollector)) {
            throw CompilationErrorException()
        }

        // Serialize klib
        if (arguments.produceKlibDir || arguments.produceKlibFile) {
            serializeFirKlib(
                moduleStructure = moduleStructure,
                firOutputs = analyzedOutput.output,
                fir2IrActualizedResult = fir2IrActualizedResult,
                outputKlibPath = outputKlibPath,
                messageCollector = messageCollector,
                diagnosticsReporter = diagnosticsReporter,
                jsOutputName = arguments.moduleName
            )
        }

        return moduleStructure
    }

    public override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2WasmCompilerArguments,
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
                sourceMapSourceRoots = calculateSourceMapSourceRoot(messageCollector, arguments.freeArgs)
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

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.friendModulesDisabled)

        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        // K/Wasm support ES modules only.
        configuration.put(JSConfigurationKeys.MODULE_KIND, ModuleKind.ES)

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

        configuration.put(JSConfigurationKeys.PRINT_REACHABILITY_INFO, arguments.dcePrintReachabilityInfo)
        configuration.put(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
        configuration.putIfNotNull(JSConfigurationKeys.DUMP_REACHABILITY_INFO_TO_FILE, arguments.dceDumpReachabilityInfoToFile)

        configuration.setupPartialLinkageConfig(
            mode = arguments.partialLinkageMode,
            logLevel = arguments.partialLinkageLogLevel,
            compilerModeAllowsUsingPartialLinkage = /* disabled for WASM for now */ false,
            onWarning = { messageCollector.report(WARNING, it) },
            onError = { messageCollector.report(ERROR, it) }
        )
    }

    override fun executableScriptFileName(): String {
        TODO("Provide a proper way to run the compiler with Wasm")
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return KlibMetadataVersion(*versionArray)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2WasmCompilerArguments) {}

    companion object {
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
            doMain(K2WasmCompiler(), args)
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