/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.extensions.ScriptEvaluationExtension
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ic.buildCache
import org.jetbrains.kotlin.ir.backend.js.ic.checkCaches
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.util.Logger
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

    override val performanceManager: CommonCompilerPerformanceManager =
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
            val projectEnv = KotlinCoreEnvironment.ProjectEnvironment(rootDisposable, environment)
            projectEnv.registerExtensionsFromPlugins(configuration)

            val scriptingEvaluators = ScriptEvaluationExtension.getInstances(projectEnv.project)
            val scriptingEvaluator = scriptingEvaluators.find { it.isAccepted(arguments) }
            if (scriptingEvaluator == null) {
                messageCollector.report(ERROR, "Unable to evaluate script, no scripting plugin loaded")
                return COMPILATION_ERROR
            }

            return scriptingEvaluator.eval(arguments, configuration, projectEnv)
        }

        if (arguments.freeArgs.isEmpty() && !IncrementalCompilation.isEnabledForJs()) {
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

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg))
        }

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        val projectJs = environmentForJS.project
        val configurationJs = environmentForJS.configuration
        val sourcesFiles = environmentForJS.getSourceFiles()

        configurationJs.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)

        if (!checkKotlinPackageUsage(environmentForJS, sourcesFiles)) return ExitCode.COMPILATION_ERROR

        val outputFilePath = arguments.outputFile
        if (outputFilePath == null) {
            messageCollector.report(ERROR, "IR: Specify output file via -output", null)
            return ExitCode.COMPILATION_ERROR
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR
        }

        if (sourcesFiles.isEmpty() && !IncrementalCompilation.isEnabledForJs() && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return COMPILATION_ERROR
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        val outputFile = File(outputFilePath)

        val moduleName = arguments.irModuleName ?: FileUtil.getNameWithoutExtension(outputFile)
        configurationJs.put(
            CommonConfigurationKeys.MODULE_NAME,
            moduleName
        )

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

        val resolvedLibraries = jsResolveLibraries(
            libraries,
            configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            messageCollectorLogger(configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY] ?: error("Could not find message collector"))
        )

        val friendAbsolutePaths = friendLibraries.map { File(it).absolutePath }
        val friendDependencies = resolvedLibraries.getFullList().filter {
            it.libraryFile.absolutePath in friendAbsolutePaths
        }

        val icCaches = configureLibraries(arguments.cacheDirectories)

        if (arguments.irBuildCache) {
            messageCollector.report(INFO, "")
            messageCollector.report(INFO, "Building cache:")
            messageCollector.report(INFO, "to: ${outputFilePath}")
            messageCollector.report(INFO, arguments.cacheDirectories ?: "")
            messageCollector.report(INFO, resolvedLibraries.getFullList().map { it.libraryName }.toString())

            val includes = arguments.includes!!

            // TODO: deduplicate
            val mainModule = run {
                if (sourcesFiles.isNotEmpty()) {
                    messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
                }
                val allLibraries = resolvedLibraries.getFullList()
                val mainLib = allLibraries.find { it.libraryFile.absolutePath == File(includes).absolutePath }!!
                MainModule.Klib(mainLib)
            }

            val start = System.currentTimeMillis()
            buildCache(
                cachePath = outputFilePath,
                project = projectJs,
                mainModule = mainModule,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                allDependencies = resolvedLibraries,
                friendDependencies = friendDependencies,
                icCache = checkCaches(resolvedLibraries, icCaches, skipLib = mainModule.lib.libraryFile.absolutePath)
            )

            messageCollector.report(INFO, "IC cache building duration: ${System.currentTimeMillis() - start}ms")
            return OK
        }

        if (arguments.irProduceKlibDir || arguments.irProduceKlibFile) {
            if (arguments.irProduceKlibFile) {
                require(outputFile.extension == KLIB_FILE_EXTENSION) { "Please set up .klib file as output" }
            }

            generateKLib(
                project = config.project,
                files = sourcesFiles,
                analyzer = AnalyzerWithCompilerReport(config.configuration),
                configuration = config.configuration,
                allDependencies = resolvedLibraries,
                friendDependencies = friendDependencies,
                irFactory = PersistentIrFactory(), // TODO IrFactoryImpl?
                outputKlibPath = outputFile.path,
                nopack = arguments.irProduceKlibDir,
                jsOutputName = arguments.irPerModuleOutputName,
            )
        }

        if (arguments.irProduceJs) {
            messageCollector.report(INFO,"Produce executable: $outputFilePath")
            messageCollector.report(INFO, arguments.cacheDirectories ?: "")

            val phaseConfig = createPhaseConfig(jsPhases, arguments, messageCollector)

            val includes = arguments.includes

            val mainModule = if (includes != null) {
                if (sourcesFiles.isNotEmpty()) {
                    messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
                }
                val allLibraries = resolvedLibraries.getFullList()
                val mainLib = allLibraries.find { it.libraryFile.absolutePath == File(includes).absolutePath }!!
                MainModule.Klib(mainLib)
            } else {
                MainModule.SourceFiles(sourcesFiles)
            }

            if (arguments.wasm) {
                val res = compileWasm(
                    projectJs,
                    mainModule,
                    AnalyzerWithCompilerReport(config.configuration),
                    config.configuration,
                    PhaseConfig(wasmPhases),
                    IrFactoryImpl,
                    allDependencies = resolvedLibraries,
                    friendDependencies = friendDependencies,
                    exportedDeclarations = setOf(FqName("main"))
                )
                val outputWasmFile = outputFile.withReplacedExtensionOrNull(outputFile.extension, "wasm")!!
                outputWasmFile.writeBytes(res.wasm)
                val outputWatFile = outputFile.withReplacedExtensionOrNull(outputFile.extension, "wat")!!
                outputWatFile.writeText(res.wat)

                val runner = """
                    const wasmBinary = read(String.raw`${outputWasmFile.absoluteFile}`, 'binary');
                    const wasmModule = new WebAssembly.Module(wasmBinary);
                    const wasmInstance = new WebAssembly.Instance(wasmModule, { runtime, js_code });
                    wasmInstance.exports.main();
                """.trimIndent()

                outputFile.writeText(res.js + "\n" + runner)
                return OK
            }

            val start = System.currentTimeMillis()

            val compiledModule = compile(
                projectJs,
                mainModule,
                AnalyzerWithCompilerReport(config.configuration),
                config.configuration,
                phaseConfig,
                if (arguments.irDceDriven) PersistentIrFactory() else IrFactoryImpl,
                allDependencies = resolvedLibraries,
                friendDependencies = friendDependencies,
                mainArguments = mainCallArguments,
                generateFullJs = !arguments.irDce,
                generateDceJs = arguments.irDce,
                dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irDceRuntimeDiagnostic,
                    messageCollector
                ),
                dceDriven = arguments.irDceDriven,
                multiModule = arguments.irPerModule,
                relativeRequirePath = true,
                propertyLazyInitialization = arguments.irPropertyLazyInitialization,
                legacyPropertyAccess = arguments.irLegacyPropertyAccess,
                baseClassIntoMetadata = arguments.irBaseClassInMetadata,
                safeExternalBoolean = arguments.irSafeExternalBoolean,
                safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irSafeExternalBooleanDiagnostic,
                    messageCollector
                ),
                lowerPerModule = icCaches.isNotEmpty(),
                useStdlibCache = icCaches.isNotEmpty(),
                icCache = if (icCaches.isNotEmpty()) checkCaches(resolvedLibraries, icCaches, skipLib = (mainModule as MainModule.Klib).lib.libraryFile.absolutePath).data else emptyMap(),
            )

            messageCollector.report(INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")

            val outputs = if (arguments.irDce && !arguments.irDceDriven) compiledModule.outputsAfterDce!! else compiledModule.outputs!!
            outputFile.write(outputs)
            outputs.dependencies.forEach { (name, content) ->
                outputFile.resolveSibling("$name.js").write(content)
            }
            if (arguments.generateDts) {
                val dtsFile = outputFile.withReplacedExtensionOrNull(outputFile.extension, "d.ts")!!
                dtsFile.writeText(compiledModule.tsDefinitions ?: error("No ts definitions"))
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
            K2JsArgumentConstants.MODULE_UMD to ModuleKind.UMD
        )
        private val sourceMapContentEmbeddingMap = mapOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS to SourceMapSourceEmbedding.ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER to SourceMapSourceEmbedding.NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING to SourceMapSourceEmbedding.INLINING
        )
        private val produceMap = mapOf(
            null to ProduceKind.DEFAULT,
            "js" to ProduceKind.JS,
            "klib" to ProduceKind.KLIB
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

fun messageCollectorLogger(collector: MessageCollector) = object : Logger {
    override fun warning(message: String) = collector.report(STRONG_WARNING, message)
    override fun error(message: String) = collector.report(ERROR, message)
    override fun log(message: String) = collector.report(LOGGING, message)
    override fun fatal(message: String): Nothing {
        collector.report(ERROR, message)
        (collector as? GroupingMessageCollector)?.flush()
        kotlin.error(message)
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
