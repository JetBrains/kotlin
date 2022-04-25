/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.wasm.WasmLoaderKind
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformerTmp
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.SourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.fileUtils.withReplacedExtensionOrNull
import org.jetbrains.kotlin.utils.join
import java.io.File
import java.io.IOException

enum class ProduceKind {
    DEFAULT,  // Determine what to produce based on js-v1 options
    JS,
    KLIB
}

class K2JsIrCompiler : CLICompiler<K2JSCompilerArguments>() {

    override val defaultPerformanceManager: CommonCompilerPerformanceManager =
        object : CommonCompilerPerformanceManager("Kotlin to JS (IR) Compiler") {}

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    override fun doExecute(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        if (configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            messageCollector.report(ERROR, "K2 does not support JS target right now")
            return ExitCode.COMPILATION_ERROR
        }

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        //TODO: add to configuration everything that may come in handy at script compiler and use it there
        if (arguments.script) {

            if (!arguments.enableJsScripting) {
                messageCollector.report(ERROR, "Script for K/JS should be enabled explicitly, see -Xenable-js-scripting")
                return COMPILATION_ERROR
            }

            configuration.put(CommonConfigurationKeys.MODULE_NAME, "repl.kts")

            val environment = KotlinCoreEnvironment.getOrCreateApplicationEnvironmentForProduction(rootDisposable, configuration)
            val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(rootDisposable, environment, configuration)
            projectEnv.registerExtensionsFromPlugins(configuration)

            val scriptingEvaluators = ScriptEvaluationExtension.getInstances(projectEnv.project)
            val scriptingEvaluator = scriptingEvaluators.find { it.isAccepted(arguments) }
            if (scriptingEvaluator == null) {
                messageCollector.report(ERROR, "Unable to evaluate script, no scripting plugin loaded")
                return COMPILATION_ERROR
            }

            return scriptingEvaluator.eval(arguments, configuration, projectEnv)
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
        val repositories: List<String> = configureLibraries(arguments.repositries)

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.REPOSITORIES, repositories)

        configuration.put(JSConfigurationKeys.PARTIAL_LINKAGE, arguments.partialLinkage)

        configuration.put(JSConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg))
        }

        arguments.relativePathBases?.let {
            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, it.toList())
        }

        configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, arguments.normalizeAbsolutePath)

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        val projectJs = environmentForJS.project
        val configurationJs = environmentForJS.configuration
        val sourcesFiles = environmentForJS.getSourceFiles()

        configurationJs.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configurationJs.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)
        configurationJs.put(JSConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, arguments.irPropertyLazyInitialization)

        if (!checkKotlinPackageUsage(environmentForJS.configuration, sourcesFiles)) return ExitCode.COMPILATION_ERROR

        val outputFilePath = arguments.outputFile
        if (outputFilePath == null) {
            messageCollector.report(ERROR, "IR: Specify output file via -output", null)
            return ExitCode.COMPILATION_ERROR
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR
        }

        if (sourcesFiles.isEmpty() && (!incrementalCompilationIsEnabledForJs(arguments)) && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return COMPILATION_ERROR
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        val outputFile = File(outputFilePath)

        val moduleName = arguments.irModuleName ?: FileUtil.getNameWithoutExtension(outputFile)
        configurationJs.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        // TODO: in this method at least 3 different compiler configurations are used (original, env.configuration, jsConfig.configuration)
        // Such situation seems a bit buggy...
        val config = JsConfig(projectJs, configurationJs, CompilerEnvironment)
        val outputDir: File = outputFile.parentFile ?: outputFile.absoluteFile.parentFile!!
        try {
            config.configuration.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return ExitCode.COMPILATION_ERROR
        }

        // TODO: Handle non-empty main call arguments
        val mainCallArguments = if (K2JsArgumentConstants.NO_CALL == arguments.main) null else emptyList<String>()

        val cacheDirectories = configureLibraries(arguments.cacheDirectories)

        val icCaches = if (cacheDirectories.isNotEmpty()) {
            messageCollector.report(INFO, "")
            messageCollector.report(INFO, "Building cache:")
            messageCollector.report(INFO, "to: ${outputFilePath}")
            messageCollector.report(INFO, arguments.cacheDirectories ?: "")
            messageCollector.report(INFO, libraries.toString())

            val includes = arguments.includes!!

            var start = System.currentTimeMillis()

            val cacheUpdater = CacheUpdater(
                includes,
                libraries,
                configurationJs,
                cacheDirectories,
                { IrFactoryImplForJsIC(WholeWorldStageController()) },
                mainCallArguments,
                ::buildCacheForModuleFiles
            )
            cacheUpdater.actualizeCaches { updateStatus, updatedModule ->
                val now = System.currentTimeMillis()
                fun reportCacheStatus(status: String, removed: Set<String> = emptySet(), updated: Set<String> = emptySet()) {
                    messageCollector.report(INFO, "IC per-file is $status duration ${now - start}ms; module [${File(updatedModule).name}]")
                    removed.forEach { messageCollector.report(INFO, "  Removed: $it") }
                    updated.forEach { messageCollector.report(INFO, "  Updated: $it") }
                }
                when (updateStatus) {
                    is CacheUpdateStatus.FastPath -> reportCacheStatus("up-to-date; fast check")
                    is CacheUpdateStatus.NoDirtyFiles -> reportCacheStatus("up-to-date; full check", updateStatus.removed)
                    is CacheUpdateStatus.Dirty -> {
                        var updated = updateStatus.updated
                        val status = StringBuilder("dirty").apply {
                            if (updateStatus.updatedAll) {
                                append("; all ${updated.size} sources updated")
                                updated = emptySet()
                            }
                            append("; cache building")
                        }.toString()
                        reportCacheStatus(status, updateStatus.removed, updated)
                    }
                }
                start = now
            }
        } else emptyList()

        // Run analysis if main module is sources
        lateinit var sourceModule: ModulesStructure
        val includes = arguments.includes
        if (includes == null) {
            do {
                sourceModule = prepareAnalyzedSourceModule(
                    projectJs,
                    environmentForJS.getSourceFiles(),
                    configurationJs,
                    libraries,
                    friendLibraries,
                    AnalyzerWithCompilerReport(config.configuration)
                )
                val result = sourceModule.jsFrontEndResult.jsAnalysisResult
                if (result is JsAnalysisResult.RetryWithAdditionalRoots) {
                    environmentForJS.addKotlinSourceRoots(result.additionalKotlinRoots)
                }
            } while (result is JsAnalysisResult.RetryWithAdditionalRoots)
            if (!sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode)
                return OK
        }

        if (arguments.irProduceKlibDir || arguments.irProduceKlibFile) {
            if (arguments.irProduceKlibFile) {
                require(outputFile.extension == KLIB_FILE_EXTENSION) { "Please set up .klib file as output" }
            }

            generateKLib(
                sourceModule,
                irFactory = IrFactoryImpl,
                outputKlibPath = outputFile.path,
                nopack = arguments.irProduceKlibDir,
                jsOutputName = arguments.irPerModuleOutputName,
            )
        }

        if (arguments.irProduceJs) {
            messageCollector.report(INFO, "Produce executable: $outputFilePath")
            messageCollector.report(INFO, arguments.cacheDirectories ?: "")

            if (icCaches.isNotEmpty()) {
                val beforeIc2Js = System.currentTimeMillis()

                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = moduleName,
                    moduleKind = configurationJs[JSConfigurationKeys.MODULE_KIND]!!,
                    sourceMapsInfo = SourceMapsInfo.from(configurationJs),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val outputs = jsExecutableProducer.buildExecutable(
                    multiModule = arguments.irPerModule,
                    rebuildCallback = { rebuiltModule ->
                        messageCollector.report(INFO, "IC module builder rebuilt module [${File(rebuiltModule).name}]")
                    }
                )

                outputFile.write(outputs)
                outputs.dependencies.forEach { (name, content) ->
                    outputFile.resolveSibling("$name.js").write(content)
                }

                messageCollector.report(INFO, "Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")

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
                sourceModule
            }


            if (arguments.wasm) {
                val (allModules, backendContext) = compileToLoweredIr(
                    depsDescriptors = module,
                    phaseConfig = PhaseConfig(wasmPhases),
                    irFactory = IrFactoryImpl,
                    exportedDeclarations = setOf(FqName("main")),
                    propertyLazyInitialization = arguments.irPropertyLazyInitialization,
                )
                if (arguments.irDce) {
                    eliminateDeadDeclarations(allModules, backendContext)
                }
                val res = compileWasm(
                    allModules = allModules,
                    backendContext = backendContext,
                    emitNameSection = arguments.wasmDebug,
                    allowIncompleteImplementations = arguments.irDce,
                )

                val launcherKind = when (arguments.wasmLauncher) {
                    "esm" -> WasmLoaderKind.BROWSER
                    "nodejs" -> WasmLoaderKind.NODE
                    "d8" -> WasmLoaderKind.D8
                    else -> throw IllegalArgumentException("Unrecognized flavor for the wasm launcher")
                }

                writeCompilationResult(
                    result = res,
                    dir = outputFile.parentFile,
                    loaderKind = launcherKind,
                    fileNameBase = outputFile.nameWithoutExtension
                )

                return OK
            }

            val start = System.currentTimeMillis()

            val granularity = when {
                arguments.irPerModule -> JsGenerationGranularity.PER_MODULE
                arguments.irPerFile -> JsGenerationGranularity.PER_FILE
                else -> JsGenerationGranularity.WHOLE_PROGRAM
            }
            try {
                val irFactory = when {
                    arguments.irNewIr2Js -> IrFactoryImplForJsIC(WholeWorldStageController())
                    else -> IrFactoryImpl
                }

                val ir = compile(
                    module,
                    phaseConfig,
                    irFactory,
                    dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                        arguments.irDceRuntimeDiagnostic,
                        messageCollector
                    ),
                    baseClassIntoMetadata = arguments.irBaseClassInMetadata,
                    safeExternalBoolean = arguments.irSafeExternalBoolean,
                    safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                        arguments.irSafeExternalBooleanDiagnostic,
                        messageCollector
                    ),
                    granularity = granularity,
                    icCompatibleIr2Js = arguments.irNewIr2Js,
                )

                val compiledModule: CompilerResult = if (arguments.irNewIr2Js) {
                    val transformer = IrModuleToJsTransformerTmp(
                        ir.context,
                        mainCallArguments,
                        relativeRequirePath = true,
                        moduleToName = ir.moduleFragmentToUniqueName
                    )

                    transformer.generateModule(
                        ir.allModules,
                        setOf(TranslationMode.fromFlags(arguments.irDce, arguments.irPerModule, arguments.irMinimizedMemberNames))
                    )
                } else {
                    val transformer = IrModuleToJsTransformer(
                        ir.context,
                        mainCallArguments,
                        fullJs = !arguments.irDce,
                        dceJs = arguments.irDce,
                        multiModule = arguments.irPerModule,
                        relativeRequirePath = true,
                        moduleToName = ir.moduleFragmentToUniqueName
                    )

                    transformer.generateModule(ir.allModules)
                }

                messageCollector.report(INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")

                val outputs = compiledModule.outputs.values.single()

                outputFile.write(outputs)
                outputs.dependencies.forEach { (name, content) ->
                    outputFile.resolveSibling("$name.js").write(content)
                }
                if (arguments.generateDts) {
                    val dtsFile = outputFile.withReplacedExtensionOrNull(outputFile.extension, "d.ts")!!
                    dtsFile.writeText(compiledModule.tsDefinitions ?: error("No ts definitions"))
                }
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
        }

        return OK
    }

    private fun File.write(outputs: CompilationOutputs) {
        writeText(outputs.jsCode)
        outputs.sourceMap?.let {
            val mapFile = resolveSibling("$name.map")
            appendText("\n//# sourceMappingURL=${mapFile.name}")
            mapFile.writeText(it)
        }
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        if (arguments.target != null) {
            assert("v5" == arguments.target) { "Unsupported ECMA version: " + arguments.target!! }
        }
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.defaultVersion())

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

        if (arguments.metaInfo) {
            configuration.put(JSConfigurationKeys.META_INFO, true)
        }

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, arguments.typedArrays)

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.friendModulesDisabled)

        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        val moduleKindName = arguments.moduleKind
        var moduleKind: ModuleKind? = if (moduleKindName != null) moduleKindMap[moduleKindName] else ModuleKind.PLAIN
        if (moduleKind == null) {
            messageCollector.report(
                ERROR, "Unknown module kind: $moduleKindName. Valid values are: plain, amd, commonjs, umd", null
            )
            moduleKind = ModuleKind.PLAIN
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)

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
            val message = "Unknown source map source embedding mode: " + sourceMapEmbedContentString + ". Valid values are: " +
                    StringUtil.join(sourceMapContentEmbeddingMap.keys, ", ")
            messageCollector.report(ERROR, message, null)
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapContentEmbedding)

        if (!arguments.sourceMap && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null)
        }

        configuration.put(JSConfigurationKeys.PRINT_REACHABILITY_INFO, arguments.irDcePrintReachabilityInfo)
        configuration.put(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
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

        @JvmStatic
        fun main(args: Array<String>) {
            CLITool.doMain(K2JsIrCompiler(), args)
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
    var pluginClasspaths: Iterable<String> = emptyList()
    val kotlinPaths = PathUtil.kotlinPathsForCompiler
    val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
    val (jars, _) =
        PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.mapNotNull { File(libPath, it) }.partition { it.exists() }
    pluginClasspaths = jars.map { it.canonicalPath } + pluginClasspaths

    return PluginCliParser.loadPluginsSafe(pluginClasspaths, mutableListOf(), configuration)
}
