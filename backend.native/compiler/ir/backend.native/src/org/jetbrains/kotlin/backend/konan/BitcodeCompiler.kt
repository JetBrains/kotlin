/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.parseBitcodeFile
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

internal class BitcodeCompiler(val context: Context) {

    private val platform = context.config.platform
    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { !it.isEmpty() })
    }

    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun temporary(name: String, suffix: String): String =
            context.config.tempFiles.create(name, suffix).absolutePath

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = if (platform.configurables is AppleConfigurables) {
            "${platform.absoluteTargetToolchain}/usr/bin/$tool"
        } else {
            "${platform.absoluteTargetToolchain}/bin/$tool"
        }
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun opt(optFlags: OptFlags, bitcodeFile: BitcodeFile): BitcodeFile {
        val flags = (optFlags.optFlags + when {
            optimize -> optFlags.optOptFlags
            debug -> optFlags.optDebugFlags
            else -> optFlags.optNooptFlags
        } + llvmProfilingFlags()).toTypedArray()
        val optimizedBc = temporary("opt_output", ".bc")
        hostLlvmTool("opt", bitcodeFile, "-o", optimizedBc, *flags)

        if (shouldRunLateBitcodePasses(context)) {
            val module = parseBitcodeFile(optimizedBc)
            runLateBitcodePasses(context, module)
            LLVMWriteBitcodeToFile(module, optimizedBc)
        }

        return optimizedBc
    }

    private fun llc(llcFlags: LlcFlags, bitcodeFile: BitcodeFile): ObjectFile {
        val flags = (llcFlags.llcFlags + when {
            optimize -> llcFlags.llcOptFlags
            debug -> llcFlags.llcDebugFlags
            else -> llcFlags.llcNooptFlags
        } + llvmProfilingFlags()).toTypedArray()
        val combinedO = temporary("llc_output", ".o")
        hostLlvmTool("llc", bitcodeFile, "-o", combinedO, *flags, "-filetype=obj")
        return combinedO
    }

    private fun bitcodeToWasm(configurables: WasmConfigurables, file: BitcodeFile): String {
        val optimizedBc = opt(configurables, file)
        val compiled = llc(configurables, optimizedBc)

        // TODO: should be moved to linker.
        val linkedWasm = temporary("linked", ".wasm")
        hostLlvmTool("wasm-ld", compiled, "-o", linkedWasm, *configurables.lldFlags.toTypedArray())

        return linkedWasm
    }

    private fun optAndLlc(configurables: ZephyrConfigurables, file: BitcodeFile): String {
        val optimizedBc = temporary("optimized", ".bc")
        val optFlags = llvmProfilingFlags() + listOf("-O3", "-internalize", "-globaldce")
        hostLlvmTool("opt", file, "-o=$optimizedBc", *optFlags.toTypedArray())

        val combinedO = temporary("combined", ".o")
        val llcFlags = llvmProfilingFlags() + listOf("-function-sections", "-data-sections")
        hostLlvmTool("llc", optimizedBc, "-filetype=obj", "-o", combinedO, *llcFlags.toTypedArray())

        return combinedO
    }

    private fun clang(configurables: ClangFlags, file: BitcodeFile): ObjectFile {
        val objectFile = temporary("result", ".o")

        val profilingFlags = llvmProfilingFlags().map { listOf("-mllvm", it) }.flatten()

        // LLVM we use does not have support for arm64_32.
        // TODO: fix with LLVM update.
        val targetTriple = when (context.config.target) {
            KonanTarget.WATCHOS_ARM64 -> {
                require(configurables is AppleConfigurables)
                "arm64_32-apple-watchos${configurables.osVersionMin}"
            }
            else -> context.llvm.targetTriple
        }
        val flags = mutableListOf<String>().apply {
            addNonEmpty(configurables.clangFlags)
            addNonEmpty(listOf("-triple", targetTriple))
            addNonEmpty(when {
                optimize -> configurables.clangOptFlags
                debug -> configurables.clangDebugFlags
                else -> configurables.clangNooptFlags
            })
            addNonEmpty(BitcodeEmbedding.getClangOptions(context.config))
            if (determineLinkerOutput(context) == LinkerOutputKind.DYNAMIC_LIBRARY) {
                addNonEmpty(configurables.clangDynamicFlags)
            }
            addNonEmpty(profilingFlags)
        }
        if (configurables is AppleConfigurables) {
            targetTool("clang++", *flags.toTypedArray(), file, "-o", objectFile)
        } else {
            hostLlvmTool("clang++", *flags.toTypedArray(), file, "-o", objectFile)
        }
        return objectFile
    }

    // llvm-lto, opt and llc share same profiling flags, so we can
    // reuse this function.
    private fun llvmProfilingFlags(): List<String> {
        val flags = mutableListOf<String>()
        if (context.shouldProfilePhases()) {
            flags += "-time-passes"
        }
        if (context.inVerbosePhase) {
            flags += "-debug-pass=Structure"
        }
        return flags
    }

    fun makeObjectFiles(bitcodeFile: BitcodeFile): List<ObjectFile> =
            listOf(when (val configurables = platform.configurables) {
                is ClangFlags ->
                    clang(configurables, bitcodeFile)
                is WasmConfigurables ->
                    bitcodeToWasm(configurables, bitcodeFile)
                is ZephyrConfigurables ->
                    optAndLlc(configurables, bitcodeFile)
                else ->
                    error("Unsupported configurables kind: ${configurables::class.simpleName}!")
            })
}