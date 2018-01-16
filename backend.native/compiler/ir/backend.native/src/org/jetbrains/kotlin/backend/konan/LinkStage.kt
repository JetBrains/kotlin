/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.createTempFile
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

internal class LinkStage(val context: Context) {

    private val config = context.config.configuration
    private val target = context.config.target
    private val platform = context.config.platform
    private val linker = platform.linker

    private val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    private val debug = config.get(KonanConfigKeys.DEBUG) ?: false
    private val dynamic = context.config.produce == CompilerOutputKind.DYNAMIC ||
            context.config.produce == CompilerOutputKind.FRAMEWORK
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
        when {
            optimize -> command.addNonEmpty(platform.llvmLtoOptFlags)
            debug    -> command.addNonEmpty(platform.llvmDebugOptFlags)
            else     -> command.addNonEmpty(platform.llvmLtoNooptFlags)
        }
        command.addNonEmpty(platform.llvmLtoDynamicFlags)
        command.addNonEmpty(files)
        runTool(command)

        return combined
    }

    private fun temporary(name: String, suffix: String): String {
        val temporaryFile = createTempFile(name, suffix)
        temporaryFile.deleteOnExit()
        return temporaryFile.absolutePath
    }

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteTargetToolchain}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    // TODO: pass different options llvm toolchain
    private fun bitcodeToWasm(bitcodeFiles: List<BitcodeFile>): String {
        val configurables = platform.configurables as WasmConfigurables

        val combinedBc = temporary("combined", ".bc")
        hostLlvmTool("llvm-link", *bitcodeFiles.toTypedArray(), "-o", combinedBc)

        val optFlags = (configurables.optFlags + when {
            optimize    -> configurables.optOptFlags
            debug       -> configurables.optDebugFlags
            else        -> configurables.optNooptFlags
        }).toTypedArray()
        val optimizedBc = temporary("optimized", ".bc")
        hostLlvmTool("opt", combinedBc, "-o", optimizedBc, *optFlags)

        val llcFlags = (configurables.llcFlags + when {
            optimize    -> configurables.llcOptFlags
            debug       -> configurables.llcDebugFlags
            else        -> configurables.llcNooptFlags
        }).toTypedArray()
        val combinedS = temporary("combined", ".s")
        targetTool("llc", optimizedBc, "-o", combinedS, *llcFlags)

        val s2wasmFlags = configurables.s2wasmFlags.toTypedArray()
        val combinedWast = temporary( "combined", ".wast")
        targetTool("s2wasm", combinedS, "-o", combinedWast, *s2wasmFlags)

        val combinedWasm = temporary( "combined", ".wasm")
        val combinedSmap = temporary( "combined", ".smap")
        targetTool("wasm-as", combinedWast, "-o", combinedWasm, "-g", "-s", combinedSmap)

        return combinedWasm
    }

    private fun llvmLinkAndLlc(bitcodeFiles: List<BitcodeFile>): String {
        val combinedBc = temporary("combined", ".bc")
        hostLlvmTool("llvm-link", "-o", combinedBc, *bitcodeFiles.toTypedArray())

        val optimizedBc = temporary("optimized", ".bc")
        hostLlvmTool("opt", combinedBc, "-o=$optimizedBc", "-O3")

        val combinedO = temporary("combined", ".o")
        hostLlvmTool("llc", combinedBc, "-filetype=obj", "-o", combinedO, "-function-sections", "-data-sections")

        return combinedO
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
        get() = if (nomain || dynamic) emptyList() else platform.entrySelector

    private fun link(objectFiles: List<ObjectFile>, includedBinaries: List<String>, libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val frameworkLinkerArgs: List<String>
        val executable: String

        if (context.config.produce != CompilerOutputKind.FRAMEWORK) {
            frameworkLinkerArgs = emptyList()
            executable = context.config.outputFile
        } else {
            val framework = File(context.config.outputFile)
            val dylibName = framework.name.removeSuffix(".framework")
            val dylibRelativePath = when (target) {
                KonanTarget.IPHONE, KonanTarget.IPHONE_SIM -> dylibName
                KonanTarget.MACBOOK -> "Versions/A/$dylibName"
                else -> error(target)
            }
            frameworkLinkerArgs = listOf("-install_name", "@rpath/${framework.name}/$dylibRelativePath")
            val dylibPath = framework.child(dylibRelativePath)
            dylibPath.parentFile.mkdirs()
            executable = dylibPath.absolutePath
        }

        try {
            linker.linkCommand(objectFiles, executable, optimize, debug, dynamic).apply {
                + linker.targetLibffi
                + asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS))
                + entryPointSelector
                + frameworkLinkerArgs
                + linker.linkCommandSuffix()
                + linker.linkStaticLibraries(includedBinaries)
                + libraryProvidedLinkerFlags
                logger = context::log
            }.execute()

            if (debug && linker is MacOSBasedLinker) {
                val outputDsymBundle = context.config.outputFile + ".dSYM" // `outputFile` is either binary or bundle.

                linker.dsymUtilCommand(executable, outputDsymBundle)
                        .logWith(context::log)
                        .execute()
            }
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("${e.toolName} invocation reported errors")
            return null
        }
        return executable
    }

    fun linkStage() {
        val bitcodeFiles = listOf(emitted) +
            libraries.map{it -> it.bitcodePaths}.flatten()

        val includedBinaries = 
            libraries.map{it -> it.includedPaths}.flatten()

        val libraryProvidedLinkerFlags = 
            libraries.map{it -> it.linkerOpts}.flatten()

        val objectFiles: MutableList<String> = mutableListOf()

        val phaser = PhaseManager(context)
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

