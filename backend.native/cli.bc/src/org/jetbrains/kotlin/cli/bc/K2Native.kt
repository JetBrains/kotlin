/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.cli.bc

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.konan.CURRENT
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.util.*
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.serialization.konan.KonanMetadataVersion

private class K2NativeCompilerPerformanceManager: CommonCompilerPerformanceManager("Kotlin to Native Compiler")
class K2Native : CLICompiler<K2NativeCompilerArguments>() {
    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = KonanMetadataVersion(*versionArray)

    override val performanceManager:CommonCompilerPerformanceManager by lazy {
        K2NativeCompilerPerformanceManager()
    }

    override fun doExecute(@NotNull arguments: K2NativeCompilerArguments,
                           @NotNull configuration: CompilerConfiguration,
                           @NotNull rootDisposable: Disposable,
                           @Nullable paths: KotlinPaths?): ExitCode {

        if (arguments.version) {
            println("Kotlin/Native: ${KonanVersion.CURRENT}")
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
            configuration.put(CommonConfigurationKeys.METADATA_VERSION, KonanMetadataVersion.INSTANCE)
        }

        try {
            runTopLevelPhases(konanConfig, environment)
        } catch (e: KonanCompilationException) {
            return ExitCode.COMPILATION_ERROR
        } catch (e: Throwable) {
            configuration.report(ERROR, """
                |Compilation failed: ${e.message}

                | * Source files: ${environment.getSourceFiles().joinToString(transform = KtFile::getName)}
                | * Compiler version info: Konan: ${KonanVersion.CURRENT} / Kotlin: ${KotlinVersion.CURRENT}
                | * Output kind: ${configuration.get(KonanConfigKeys.PRODUCE)}

                """.trimMargin())
            throw e
        }

        return ExitCode.OK
    }

    val K2NativeCompilerArguments.isUsefulWithoutFreeArgs: Boolean
        get() = listTargets || listPhases || checkDependencies || !includes.isNullOrEmpty()

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

                put(NODEFAULTLIBS, arguments.nodefaultlibs)
                put(NOSTDLIB, arguments.nostdlib)
                put(NOPACK, arguments.nopack)
                put(NOMAIN, arguments.nomain)
                put(LIBRARY_FILES,
                        arguments.libraries.toNonNullList())
                put(LINKER_ARGS, arguments.linkerArguments.toNonNullList() +
                        arguments.singleLinkerArguments.toNonNullList())
                arguments.moduleName ?. let{ put(MODULE_NAME, it) }
                arguments.target ?.let{ put(TARGET, it) }

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
                arguments.libraryVersion ?. let { put(LIBRARY_VERSION, it) }

                arguments.mainPackage ?.let{ put(ENTRY, it) }
                arguments.manifestFile ?.let{ put(MANIFEST_FILE, it) }
                arguments.runtimeFile ?.let{ put(RUNTIME_FILE, it) }
                arguments.temporaryFilesDir?.let { put(TEMPORARY_FILES_DIR, it) }

                put(LIST_TARGETS, arguments.listTargets)
                put(OPTIMIZATION, arguments.optimization)
                put(DEBUG, arguments.debug)
                put(LIGHT_DEBUG, arguments.lightDebug)
                put(STATIC_FRAMEWORK, selectFrameworkType(configuration, arguments, outputKind))

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

                put(BITCODE_EMBEDDING_MODE, selectBitcodeEmbeddingMode(this, arguments, outputKind))
                put(DEBUG_INFO_VERSION, arguments.debugInfoFormatVersion.toInt())
                put(COVERAGE, arguments.coverage)
                put(LIBRARIES_TO_COVER, arguments.coveredLibraries.toNonNullList())
                arguments.coverageFile?.let { put(PROFRAW_PATH, it) }
                put(OBJC_GENERICS, arguments.objcGenerics)
            }
        }
    }

    override fun createArguments() = K2NativeCompilerArguments()

    override fun executableScriptFileName() = "kotlinc-native"

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            profile("Total compiler main()") {
                CLITool.doMain(K2Native(), args)
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
        arguments: K2NativeCompilerArguments,
        outputKind: CompilerOutputKind
): BitcodeEmbedding.Mode {

    if (outputKind != CompilerOutputKind.FRAMEWORK) {
        return BitcodeEmbedding.Mode.NONE.also {
            val flag = when {
                arguments.embedBitcodeMarker -> EMBED_BITCODE_MARKER_FLAG
                arguments.embedBitcode -> EMBED_BITCODE_FLAG
                else -> return@also
            }

            configuration.report(
                    STRONG_WARNING,
                    "'$flag' is only supported when producing frameworks, " +
                            "but the compiler is producing ${outputKind.name.toLowerCase()}"
            )
        }
    }

    return when {
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
    val produceBinaryOrBitcode = outputKind.let { it.isNativeBinary || it == CompilerOutputKind.BITCODE }

    return if (includes.isNotEmpty() && !produceBinaryOrBitcode) {
        configuration.report(
            ERROR,
            "The $INCLUDE_ARG flag is only supported when producing native binaries or bitcode files, " +
                    "but the compiler is producing ${outputKind.name.toLowerCase()}"
        )
        emptyList()
    } else {
        includes
    }
}

fun main(args: Array<String>) = K2Native.main(args)

