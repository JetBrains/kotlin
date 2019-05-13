/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR
import org.jetbrains.kotlin.cli.common.ExitCode.OK
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
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
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.KlibModuleRef
import org.jetbrains.kotlin.ir.backend.js.compile
import org.jetbrains.kotlin.ir.backend.js.generateKLib
import org.jetbrains.kotlin.ir.backend.js.jsPhases
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.join
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

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

    private fun extractKlibFromZip(library: String, messageCollector: MessageCollector): File? {
        val zipLib = ZipFile(library)
        val tempDir = createTempDir(File(library).name, "klibjar")
        tempDir.deleteOnExit()
        var extractedKlibDir: File? = null
        for (entry in zipLib.entries()) {
            if (!entry.isDirectory) {
                zipLib.getInputStream(entry).use { input ->
                    val outputEntryFile = File(tempDir, entry.name)
                    outputEntryFile.parentFile.mkdirs()
                    outputEntryFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else {
                if (entry.name.endsWith("KLIB/")) {
                    extractedKlibDir = File(tempDir, entry.name)
                    messageCollector.report(INFO, "Klib $library is extracted into $extractedKlibDir")
                }
            }
        }

        return extractedKlibDir
    }

    private fun loadIrLibrary(library: String, messageCollector: MessageCollector): KlibModuleRef? {
        val libraryFile = File(library)
        var klibDir = when {
            FileUtil.isJarOrZip(libraryFile) -> {
                extractKlibFromZip(library, messageCollector) ?: return null
            }
            !libraryFile.isDirectory -> {
                messageCollector.report(ERROR, "Klib $library must be a directory")
                return null
            }
            else ->
                libraryFile
        }

        var klibFiles = klibDir.listFiles()
        if (klibFiles.isEmpty()) {
            messageCollector.report(STRONG_WARNING, "Klib $library directory is empty")
            return null
        }

        // Sometines gradle gives us a directory with klib directory inside
        val klibDirInsideDir = klibFiles.find { it.name.endsWith("KLIB") }
        if (klibDirInsideDir != null) {
            klibDir = klibDirInsideDir
            klibFiles = klibDir.listFiles()
        }

        val metadataFile = klibFiles.find { it.extension == "klm" }

        if (metadataFile == null) {
            messageCollector.report(STRONG_WARNING, "No metadata file (.klm) for klib: $klibDir")
            return null
        }

        return KlibModuleRef(metadataFile.nameWithoutExtension, klibDir.absolutePath)
    }

    override fun doExecute(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        if (arguments.freeArgs.isEmpty() && !IncrementalCompilation.isEnabledForJs()) {
            if (arguments.version) {
                return OK
            }
            messageCollector.report(ERROR, "Specify at least one source file or directory", null)
            return COMPILATION_ERROR
        }

        val pluginLoadResult = PluginCliParser.loadPluginsSafe(
            arguments.pluginClasspaths,
            arguments.pluginOptions,
            configuration
        )
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val libraries: List<String> = configureLibraries(arguments.libraries)
        val friendLibraries: List<String> = configureLibraries(arguments.friendModules)

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, libraries)

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg))
        }

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)

        val project = environmentForJS.project
        val sourcesFiles = environmentForJS.getSourceFiles()

        environmentForJS.configuration.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)

        if (!checkKotlinPackageUsage(environmentForJS, sourcesFiles)) return ExitCode.COMPILATION_ERROR

        val outputFilePath = arguments.outputFile
        if (outputFilePath == null) {
            messageCollector.report(ERROR, "IR: Specify output file via -output", null)
            return ExitCode.COMPILATION_ERROR
        }

        if (messageCollector.hasErrors()) {
            return ExitCode.COMPILATION_ERROR
        }

        if (sourcesFiles.isEmpty() && !IncrementalCompilation.isEnabledForJs()) {
            messageCollector.report(ERROR, "No source files", null)
            return COMPILATION_ERROR
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        val outputFile = File(outputFilePath)

        configuration.put(CommonConfigurationKeys.MODULE_NAME, FileUtil.getNameWithoutExtension(outputFile))

        val config = JsConfig(project, configuration)
        val outputDir: File = outputFile.parentFile ?: outputFile.absoluteFile.parentFile!!
        try {
            config.configuration.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return ExitCode.COMPILATION_ERROR
        }

        // TODO: Handle non-empty main call arguments
        val mainCallArguments = if (K2JsArgumentConstants.NO_CALL == arguments.main) null else emptyList<String>()

        val loadedLibrariesNames = mutableSetOf<String>()
        val dependencies = mutableListOf<KlibModuleRef>()
        val friendDependencies = mutableListOf<KlibModuleRef>()

        for (library in libraries) {
            val irLib = loadIrLibrary(library, messageCollector) ?: continue
            if (irLib.moduleName !in loadedLibrariesNames) {
                dependencies.add(irLib)
                loadedLibrariesNames.add(irLib.moduleName)
                if (library in friendLibraries) {
                    friendDependencies.add(irLib)
                }
            }
        }

        val produceKind = produceMap[arguments.irProduceOnly]
        if (produceKind == null) {
            messageCollector.report(ERROR, "Unknown produce kind: ${arguments.irProduceOnly}. Valid values are: js, klib")
        }

        if (produceKind == ProduceKind.JS || produceKind == ProduceKind.DEFAULT) {
            val phaseConfig = createPhaseConfig(jsPhases, arguments, messageCollector)

            val compiledModule = compile(
                project,
                sourcesFiles,
                configuration,
                phaseConfig,
                immediateDependencies = dependencies,
                allDependencies = dependencies,
                friendDependencies = friendDependencies,
                mainArguments = mainCallArguments
            )

            outputFile.writeText(compiledModule)
        }

        if (produceKind == ProduceKind.KLIB || (produceKind == ProduceKind.DEFAULT && arguments.metaInfo)) {
            val outputKlibPath = "$outputFilePath.KLIB"
            generateKLib(
                project = config.project,
                files = sourcesFiles,
                configuration = config.configuration,
                immediateDependencies = dependencies,
                allDependencies = dependencies,
                friendDependencies = friendDependencies,
                outputKlibPath = outputKlibPath
            )
        }

        return OK
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

        // TODO: Support source maps
        if (arguments.sourceMap) {
            messageCollector.report(WARNING, "source-map argument is not supported yet", null)
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

        val incrementalDataProvider = services[IncrementalDataProvider::class.java]
        if (incrementalDataProvider != null) {
            configuration.put(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER, incrementalDataProvider)
        }

        val incrementalResultsConsumer = services[IncrementalResultsConsumer::class.java]
        if (incrementalResultsConsumer != null) {
            configuration.put(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, incrementalResultsConsumer)
        }

        val lookupTracker = services[LookupTracker::class.java]
        if (lookupTracker != null) {
            configuration.put(CommonConfigurationKeys.LOOKUP_TRACKER, lookupTracker)
        }

        val expectActualTracker = services[ExpectActualTracker::class.java]
        if (expectActualTracker != null) {
            configuration.put(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, expectActualTracker)
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
    }

    override fun executableScriptFileName(): String {
        return "kotlinc-js -Xir"
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        // TODO: Support metadata versions for klibs
        return JsMetadataVersion(*versionArray)
    }

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
