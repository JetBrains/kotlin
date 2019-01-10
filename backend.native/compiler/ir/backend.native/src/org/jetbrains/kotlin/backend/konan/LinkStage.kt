/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.isBitcode
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

internal class LinkStage(val context: Context, val phaser: PhaseManager) {

    private val config = context.config.configuration
    private val target = context.config.target
    private val platform = context.config.platform
    private val linker = platform.linker

    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug
    private val linkerOutput = when (context.config.produce) {
        CompilerOutputKind.DYNAMIC, CompilerOutputKind.FRAMEWORK -> LinkerOutputKind.DYNAMIC_LIBRARY
        CompilerOutputKind.STATIC -> LinkerOutputKind.STATIC_LIBRARY
        CompilerOutputKind.PROGRAM -> LinkerOutputKind.EXECUTABLE
        else -> TODO("${context.config.produce} should not reach native linker stage")
    }
    private val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    private val emitted = context.bitcodeFileName
    private val libraries = context.llvm.librariesToLink
    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { !it.isEmpty() })
    }

    private fun runTool(command: List<String>) = runTool(*command.toTypedArray())
    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        val combined = temporary("combined", ".o")

        val tool = "${platform.absoluteLlvmHome}/bin/llvm-lto"
        val command = mutableListOf(tool, "-o", combined)
        command.addNonEmpty(platform.llvmLtoFlags)
        command.addNonEmpty(llvmProfilingFlags())
        when {
            optimize -> command.addNonEmpty(platform.llvmLtoOptFlags)
            debug -> command.addNonEmpty(platform.llvmDebugOptFlags)
            else -> command.addNonEmpty(platform.llvmLtoNooptFlags)
        }
        command.addNonEmpty(platform.llvmLtoDynamicFlags)
        command.addNonEmpty(files)
        runTool(command)

        return combined
    }

    private fun temporary(name: String, suffix: String): String =
            context.config.tempFiles.create(name, suffix).absolutePath

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteTargetToolchain}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun bitcodeToWasm(bitcodeFiles: List<BitcodeFile>): String {
        val configurables = platform.configurables as WasmConfigurables

        val combinedBc = temporary("combined", ".bc")
        // TODO: use -only-needed for the stdlib
        hostLlvmTool("llvm-link", *bitcodeFiles.toTypedArray(), "-o", combinedBc)
        val optFlags = (configurables.optFlags + when {
            optimize -> configurables.optOptFlags
            debug -> configurables.optDebugFlags
            else -> configurables.optNooptFlags
        } + llvmProfilingFlags()).toTypedArray()
        val optimizedBc = temporary("optimized", ".bc")
        hostLlvmTool("opt", combinedBc, "-o", optimizedBc, *optFlags)
        val llcFlags = (configurables.llcFlags + when {
            optimize -> configurables.llcOptFlags
            debug -> configurables.llcDebugFlags
            else -> configurables.llcNooptFlags
        } + llvmProfilingFlags()).toTypedArray()
        val combinedO = temporary("combined", ".o")
        hostLlvmTool("llc", optimizedBc, "-o", combinedO, *llcFlags, "-filetype=obj")
        val linkedWasm = temporary("linked", ".wasm")
        hostLlvmTool("wasm-ld", combinedO, "-o", linkedWasm, *configurables.lldFlags.toTypedArray())
        return linkedWasm
    }

    private fun llvmLinkAndLlc(bitcodeFiles: List<BitcodeFile>): String {
        val combinedBc = temporary("combined", ".bc")
        hostLlvmTool("llvm-link", "-o", combinedBc, *bitcodeFiles.toTypedArray())

        val optimizedBc = temporary("optimized", ".bc")
        val optFlags = llvmProfilingFlags() + listOf("-O3", "-internalize", "-globaldce")
        hostLlvmTool("opt", combinedBc, "-o=$optimizedBc", *optFlags.toTypedArray())

        val combinedO = temporary("combined", ".o")
        val llcFlags = llvmProfilingFlags() + listOf("-function-sections", "-data-sections")
        hostLlvmTool("llc", optimizedBc, "-filetype=obj", "-o", combinedO, *llcFlags.toTypedArray())

        return combinedO
    }

    // llvm-lto, opt and llc share same profiling flags, so we can
    // reuse this function.
    private fun llvmProfilingFlags(): List<String> {
        val flags = mutableListOf<String>()
        if (context.shouldProfilePhases()) {
            flags += "-time-passes"
        }
        if (context.phase?.verbose == true) {
            flags += "-debug-pass=Structure"
        }
        return flags
    }

    private fun asLinkerArgs(args: List<String>): List<String> {
        if (linker.useCompilerDriverAsLinker) {
            return args
        }

        val result = mutableListOf<String>()
        for (arg in args) {
            // If user passes compiler arguments to us - transform them to linker ones.
            if (arg.startsWith("-Wl,")) {
                result.addAll(arg.substring(4).split(','))
            } else {
                result.add(arg)
            }
        }
        return result
    }

    // Ideally we'd want to have 
    //      #pragma weak main = Konan_main
    // in the launcher.cpp.
    // Unfortunately, anything related to weak linking on MacOS
    // only seems to be working with dynamic libraries.
    // So we stick to "-alias _main _konan_main" on Mac.
    // And just do the same on Linux.
    private val entryPointSelector: List<String>
        get() = if (nomain || linkerOutput != LinkerOutputKind.EXECUTABLE) emptyList() else platform.entrySelector

    private fun link(objectFiles: List<ObjectFile>,
                     includedBinaries: List<String>,
                     libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val frameworkLinkerArgs: List<String>
        val executable: String

        if (context.config.produce != CompilerOutputKind.FRAMEWORK) {
            frameworkLinkerArgs = emptyList()
            executable = context.config.outputFile
        } else {
            val framework = File(context.config.outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target.family) {
                Family.IOS -> dylibName
                Family.OSX -> "Versions/A/$dylibName"
                else -> error(target)
            }
            frameworkLinkerArgs = listOf("-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        try {
            File(executable).delete()
            linker.linkCommands(objectFiles = objectFiles, executable = executable,
                    libraries = linker.linkStaticLibraries(includedBinaries) + context.config.defaultSystemLibraries,
                    linkerArgs = entryPointSelector +
                            asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                            BitcodeEmbedding.getLinkerOptions(context.config) +
                            libraryProvidedLinkerFlags + frameworkLinkerArgs,
                    optimize = optimize, debug = debug, kind = linkerOutput,
                    outputDsymBundle = context.config.outputFile + ".dSYM").forEach {
                it.logWith(context::log)
                it.execute()
            }
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("${e.toolName} invocation reported errors")
            return null
        }
        return executable
    }

    fun linkStage() {
        val bitcodeFiles = listOf(emitted) +
                libraries.map { it.bitcodePaths }.flatten().filter { it.isBitcode }

        val includedBinaries =
                libraries.map { it.includedPaths }.flatten()

        val libraryProvidedLinkerFlags =
                libraries.map { it.linkerOpts }.flatten()

        val objectFiles: MutableList<String> = mutableListOf()

        phaser.phase(KonanPhase.OBJECT_FILES) {
            objectFiles.add(
                    when (platform.configurables) {
                        is WasmConfigurables
                        -> bitcodeToWasm(bitcodeFiles)
                        is ZephyrConfigurables
                        -> llvmLinkAndLlc(bitcodeFiles)
                        else
                        -> llvmLto(bitcodeFiles)
                    }
            )
        }
        phaser.phase(KonanPhase.LINKER) {
            link(objectFiles, includedBinaries, libraryProvidedLinkerFlags)
        }
    }
}

