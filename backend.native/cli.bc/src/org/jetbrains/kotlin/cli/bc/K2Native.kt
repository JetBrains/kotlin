/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.CompilerVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.KotlinPaths

private class K2NativeCompilerPerformanceManager: CommonCompilerPerformanceManager("Kotlin to Native Compiler")
class K2Native : CLICompiler<K2NativeCompilerArguments>() {

    override fun MutableList<String>.addPlatformOptions(arguments: K2NativeCompilerArguments) {}

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = KlibMetadataVersion(*versionArray)

    override val performanceManager:CommonCompilerPerformanceManager by lazy {
        K2NativeCompilerPerformanceManager()
    }

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${CompilerVersion.CURRENT}")
            return ExitCode.OK
        }

        val pluginLoadResult =
            PluginCliParser.loadPluginsSafe(arguments.pluginClasspaths, arguments.pluginOptions, configuration)
        if (pluginLoadResult != ExitCode.OK) return pluginLoadResult

        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
            configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)
        val project = environment.project
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE
        configuration.put(CLIConfigurationKeys.PHASE_CONFIG, createPhaseConfig(toplevelPhase, arguments, messageCollector))
        val konanConfig = KonanConfig(project, configuration)

        val enoughArguments = arguments.freeArgs.isNotEmpty() || arguments.isUsefulWithoutFreeArgs
        if (!enoughArguments) {
            configuration.report(ERROR, "You have not specified any compilation arguments. No output has been produced.")
        }

        /* Set default version of metadata version */
        val metadataVersionString = arguments.metadataVersion
        if (metadataVersionString == null) {
            configuration.put(CommonConfigurationKeys.METADATA_VERSION, KlibMetadataVersion.INSTANCE)
        }

        try {
            runTopLevelPhases(konanConfig, environment)
        } catch (e: KonanCompilationException) {
            return ExitCode.COMPILATION_ERROR
        } catch (e: Throwable) {
            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version info: Konan: ${CompilerVersion.CURRENT} / Kotlin: ${KotlinVersion.CURRENT}
                | * Output kind: ${configuration.get(KonanConfigKeys.PRODUCE)}

                """.trimMargin())
            throw e
        }

        return ExitCode.OK
    }

    val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() = listTargets || listPhases || checkDependencies || !includes.isNullOrEmpty() ||
                !librariesToCache.isNullOrEmpty() || libraryToAddToCache != null

    fun Array<String>?.toNonNullList(): List<String> {
        return this?.asList<String>() ?: listOf<String>()
    }

    // It is executed before doExecute().
    override fun setupPlatformSpecificArgumentsAndServices(
            configuration: CompilerConfiguration,
            arguments    : K2NativeCompilerArguments,
            services     : Services) {

        val commonSources = arguments.commonSources?.toSet().orEmpty()
        arguments.freeArgs.forEach {
            configuration.addKotlinSourceRoot(it, it in commonSources)
        }

        with(KonanConfigKeys) {
            with(configuration) {
                arguments.kotlinHome?.let { put(KONAN_HOME, it) }

                put(NODEFAULTLIBS, arguments.nodefaultlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOENDORSEDLIBS, arguments.noendorsedlibs || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOSTDLIB, arguments.nostdlib || !arguments.libraryToAddToCache.isNullOrEmpty())
                put(NOPACK, arguments.nopack)
                put(NOMAIN, arguments.nomain)
                put(LIBRARY_FILES,
                        arguments.libraries.toNonNullList())
                put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
                        arguments.singleLinkerArguments.toNonNullList())
                arguments.moduleName?.let{ put(MODULE_NAME, it) }
                arguments.target?.let{ put(TARGET, it) }

                put(INCLUDED_BINARY_FILES,
                        arguments.includeBinaries.toNonNullList())
                put(NATIVE_LIBRARY_FILES,
                        arguments.nativeLibraries.toNonNullList())
                put(REPOSITORIES,
                        arguments.repositories.toNonNullList())

                // TODO: Collect all the explicit file names into an object
                // and teach the compiler to work with temporaries and -save-temps.

                arguments.outputName ?.let { put(OUTPUT, it) }
                val outputKind = CompilerOutputKind.valueOf(
                    (arguments.produce ?: "program").toUpperCase())
                put(PRODUCE, outputKind)
                put(METADATA_KLIB, arguments.metadataKlib)

                arguments.libraryVersion ?. let { put(LIBRARY_VERSION, it) }

                arguments.mainPackage ?.let{ put(ENTRY, it) }
                arguments.manifestFile ?.let{ put(MANIFEST_FILE, it) }
                arguments.runtimeFile ?.let{ put(RUNTIME_FILE, it) }
                arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }

                put(LIST_TARGETS, arguments.listTargets)
                put(OPTIMIZATION, arguments.optimization)
                put(DEBUG, arguments.debug)
                // TODO: remove after 1.4 release.
                if (arguments.lightDebugDeprecated) {
                    configuration.report(WARNING,
                            "-Xg0 is now deprecated and skipped by compiler. Light debug information is enabled by default for Darwin platforms." +
                                    " For other targets, please, use `-Xadd-light-debug=enable` instead.")
                }
                putIfNotNull(LIGHT_DEBUG, when (val it = arguments.lightDebugString) {
                    "enable" -> true
                    "disable" -> false
                    null -> null
                    else -> {
                        configuration.report(ERROR, "Unsupported -Xadd-light-debug= value: $it. Possible values are 'enable'/'disable'")
                        null
                    }
                })
                put(STATIC_FRAMEWORK, selectFrameworkType(configuration, arguments, outputKind))
                put(OVERRIDE_CLANG_OPTIONS, arguments.clangOptions.toNonNullList())
                put(ALLOCATION_MODE, arguments.allocator)

                put(PRINT_IR, arguments.printIr)
                put(PRINT_IR_WITH_DESCRIPTORS, arguments.printIrWithDescriptors)
                put(PRINT_DESCRIPTORS, arguments.printDescriptors)
                put(PRINT_LOCATIONS, arguments.printLocations)
                put(PRINT_BITCODE, arguments.printBitCode)

                put(PURGE_USER_LIBS, arguments.purgeUserLibs)

                if (arguments.verifyCompiler != null)
                    put(VERIFY_COMPILER, arguments.verifyCompiler == "true")
                put(VERIFY_BITCODE, arguments.verifyBitCode)

                put(ENABLED_PHASES,
                        arguments.enablePhases.toNonNullList())
                put(DISABLED_PHASES,
                        arguments.disablePhases.toNonNullList())
                put(LIST_PHASES, arguments.listPhases)

                put(COMPATIBLE_COMPILER_VERSIONS,
                    arguments.compatibleCompilerVersions.toNonNullList())

                put(ENABLE_ASSERTIONS, arguments.enableAssertions)

                put(MEMORY_MODEL, when (arguments.memoryModel) {
                    "relaxed" -> {
                        configuration.report(STRONG_WARNING, "Relaxed memory model is not yet fully functional")
                        MemoryModel.RELAXED
                    }
                    "strict" -> MemoryModel.STRICT
                    else -> {
                        configuration.report(ERROR, "Unsupported memory model ${arguments.memoryModel}")
                        MemoryModel.STRICT
                    }
                })

                when {
                    arguments.generateWorkerTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.WORKER)
                    arguments.generateTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD)
                    arguments.generateNoExitTestRunner -> put(GENERATE_TEST_RUNNER, TestRunnerKind.MAIN_THREAD_NO_EXIT)
                    else -> put(GENERATE_TEST_RUNNER, TestRunnerKind.NONE)
                }
                // We need to download dependencies only if we use them ( = there are files to compile).
                put(
                    CHECK_DEPENDENCIES,
                    configuration.kotlinSourceRoots.isNotEmpty()
                            || !arguments.includes.isNullOrEmpty()
                            || arguments.checkDependencies
                )
                if (arguments.friendModules != null)
                    put(FRIEND_MODULES, arguments.friendModules!!.split(File.pathSeparator).filterNot(String::isEmpty))

                put(EXPORTED_LIBRARIES, selectExportedLibraries(configuration, arguments, outputKind))
                put(INCLUDED_LIBRARIES, selectIncludes(configuration, arguments, outputKind))
                put(FRAMEWORK_IMPORT_HEADERS, arguments.frameworkImportHeaders.toNonNullList())
                arguments.emitLazyObjCHeader?.let { put(EMIT_LAZY_OBJC_HEADER_FILE, it) }

                put(BITCODE_EMBEDDING_MODE, selectBitcodeEmbeddingMode(this, arguments))
                put(DEBUG_INFO_VERSION, arguments.debugInfoFormatVersion.toInt())
                put(COVERAGE, arguments.coverage)
                put(LIBRARIES_TO_COVER, arguments.coveredLibraries.toNonNullList())
                arguments.coverageFile?.let { put(PROFRAW_PATH, it) }
                put(OBJC_GENERICS, !arguments.noObjcGenerics)
                put(DEBUG_PREFIX_MAP, parseDebugPrefixMap(arguments, configuration))

                put(LIBRARIES_TO_CACHE, parseLibrariesToCache(arguments, configuration, outputKind))
                val libraryToAddToCache = parseLibraryToAddToCache(arguments, configuration, outputKind)
                if (libraryToAddToCache != null && !arguments.outputName.isNullOrEmpty())
                    configuration.report(ERROR, "$ADD_CACHE already implicitly sets output file name")
                val cacheDirectories = arguments.cacheDirectories.toNonNullList()
                libraryToAddToCache?.let { put(LIBRARY_TO_ADD_TO_CACHE, it) }
                put(CACHE_DIRECTORIES, cacheDirectories)
                put(CACHED_LIBRARIES, parseCachedLibraries(arguments, configuration))

                parseShortModuleName(arguments, configuration, outputKind)?.let {
                    put(SHORT_MODULE_NAME, it)
                }
                put(DISABLE_FAKE_OVERRIDE_VALIDATOR, arguments.disableFakeOverrideValidator)
            }
        }
    }

    override fun createArguments() = K2NativeCompilerArguments()

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                doMain(K2Native(), args)
            }
        }
        @JvmStatic fun mainNoExit(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }

        @JvmStatic fun mainNoExitWithGradleRenderer(args: Array<String>) {
            profile("Total compiler main()") {
                if (doMainNoExit(K2Native(), args, MessageRenderer.GRADLE_STYLE) != ExitCode.OK) {
                    throw KonanCompilationException("Compilation finished with errors")
                }
            }
        }
    }
}

private fun selectFrameworkType(
    configuration: CompilerConfiguration,
    arguments: K2NativeCompilerArguments,
    outputKind: CompilerOutputKind
): Boolean {
    return if (outputKind != CompilerOutputKind.FRAMEWORK && arguments.staticFramework) {
        configuration.report(
            STRONG_WARNING,
            "'$STATIC_FRAMEWORK_FLAG' is only supported when producing frameworks, " +
            "but the compiler is producing ${outputKind.name.toLowerCase()}"
        )
        false
    } else {
       arguments.staticFramework
    }
}

private fun selectBitcodeEmbeddingMode(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments
): BitcodeEmbedding.Mode = when {
    arguments.embedBitcodeMarker -> {
        if (arguments.embedBitcode) {
            configuration.report(
                    STRONG_WARNING,
                    "'$EMBED_BITCODE_FLAG' is ignored because '$EMBED_BITCODE_MARKER_FLAG' is specified"
            )
        }
        BitcodeEmbedding.Mode.MARKER
    }
    arguments.embedBitcode -> {
        BitcodeEmbedding.Mode.FULL
    }
    else -> BitcodeEmbedding.Mode.NONE
}

private fun selectExportedLibraries(
        configuration: CompilerConfiguration,
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
): List<String> {
    val exportedLibraries = arguments.exportedLibraries?.toList().orEmpty()

    return if (exportedLibraries.isNotEmpty() && outputKind != CompilerOutputKind.FRAMEWORK &&
            outputKind != CompilerOutputKind.STATIC && outputKind != CompilerOutputKind.DYNAMIC) {
        configuration.report(STRONG_WARNING,
                "-Xexport-library is only supported when producing frameworks or native libraries, " +
                "but the compiler is producing ${outputKind.name.toLowerCase()}")

        emptyList()
    } else {
        exportedLibraries
    }
}

private fun selectIncludes(
    configuration: CompilerConfiguration,
    arguments: K2NativeCompilerArguments,
    outputKind: CompilerOutputKind
): List<String> {
    val includes = arguments.includes?.toList().orEmpty()

    return if (includes.isNotEmpty() && outputKind == CompilerOutputKind.LIBRARY) {
        configuration.report(
            ERROR,
            "The $INCLUDE_ARG flag is not supported when producing ${outputKind.name.toLowerCase()}"
        )
        emptyList()
    } else {
        includes
    }
}

private fun parseCachedLibraries(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): Map<String, String> = arguments.cachedLibraries?.asList().orEmpty().mapNotNull {
    val libraryAndCache = it.split(",")
    if (libraryAndCache.size != 2) {
        configuration.report(
                ERROR,
                "incorrect $CACHED_LIBRARY format: expected '<library>,<cache>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()

private fun parseLibrariesToCache(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): List<String> {
    val input = arguments.librariesToCache?.asList().orEmpty()

    return if (input.isNotEmpty() && !outputKind.isCache) {
        configuration.report(ERROR, "$MAKE_CACHE can't be used when not producing cache")
        emptyList()
    } else if (input.isNotEmpty() && !arguments.libraryToAddToCache.isNullOrEmpty()) {
        configuration.report(ERROR, "supplied both $MAKE_CACHE and $ADD_CACHE options")
        emptyList()
    } else {
        input
    }
}

private fun parseLibraryToAddToCache(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): String? {
    val input = arguments.libraryToAddToCache

    return if (input != null && !outputKind.isCache) {
        configuration.report(ERROR, "$ADD_CACHE can't be used when not producing cache")
        null
    } else {
        input
    }
}

// TODO: Support short names for current module in ObjC export and lift this limitation.
private fun parseShortModuleName(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration,
        outputKind: CompilerOutputKind
): String? {
    val input = arguments.shortModuleName

    return if (input != null && outputKind != CompilerOutputKind.LIBRARY) {
        configuration.report(
                STRONG_WARNING,
                "$SHORT_MODULE_NAME_ARG is only supported when producing a Kotlin library, " +
                    "but the compiler is producing ${outputKind.name.toLowerCase()}"
        )
        null
    } else {
        input
    }
}

private fun parseDebugPrefixMap(
        arguments: K2NativeCompilerArguments,
        configuration: CompilerConfiguration
): Map<String, String> = arguments.debugPrefixMap?.asList().orEmpty().mapNotNull {
    val libraryAndCache = it.split("=")
    if (libraryAndCache.size != 2) {
        configuration.report(
                ERROR,
                "incorrect debug prefix map format: expected '<old>=<new>', got '$it'"
        )
        null
    } else {
        libraryAndCache[0] to libraryAndCache[1]
    }
}.toMap()


fun main(args: Array<String>) = K2Native.main(args)
fun mainNoExitWithGradleRenderer(args: Array<String>) = K2Native.mainNoExitWithGradleRenderer(args)
