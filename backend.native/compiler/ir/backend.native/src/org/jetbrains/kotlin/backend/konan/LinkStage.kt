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

import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
internal abstract class PlatformFlags(val properties: KonanProperties) {
    val llvmLtoNooptFlags = properties.llvmLtoNooptFlags
    val llvmLtoOptFlags = properties.llvmLtoOptFlags
    val llvmLtoFlags = properties.llvmLtoFlags
    val entrySelector = properties.entrySelector
    val linkerOptimizationFlags = properties.linkerOptimizationFlags
    val linkerKonanFlags = properties.linkerKonanFlags
    val linkerDebugFlags = properties.linkerDebugFlags
    val llvmDebugOptFlags = properties.llvmDebugOptFlags
    val s2wasmFlags = properties.s2wasmFlags
    val targetToolchain = properties.absoluteTargetToolchain
    val targetSysRoot = properties.absoluteTargetSysRoot

    val targetLibffi = properties.libffiDir ?.let { listOf("${properties.absoluteLibffiDir}/lib/libffi.a") } ?: emptyList()

    open val useCompilerDriverAsLinker: Boolean get() = false // TODO: refactor.

    abstract fun linkCommand(objectFiles: List<ObjectFile>,
        executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String>

    open fun linkCommandSuffix(): List<String> = emptyList()

    protected fun propertyTargetString(name: String)
        = properties.targetString(name)!!
    protected fun propertyTargetList(name: String)
        = properties.targetList(name)
}


internal open class AndroidPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val prefix = "$targetToolchain/bin/"
    private val clang = "$prefix/clang"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String> {
        return mutableListOf(clang).apply{
            add("-o")
            add(executable)
            add("-fPIC")
            add("-shared")
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerDebugFlags)
            addAll(linkerKonanFlags)
        }
    }
}

internal open class MacOSBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    // TODO: move 'ld' out of the host sysroot, as it doesn't belong here.
    private val linker = "${distribution.hostSysRoot}/usr/bin/ld"
    internal val dsymutil = "${distribution.llvmBin}/llvm-dsymutil"
    internal val libLTO = distribution.libLTO

    open val osVersionMin by lazy {
        listOf(
                propertyTargetString("osVersionMinFlagLd"),
                propertyTargetString("osVersionMin") + ".0")
    }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String> {
        return mutableListOf(linker).apply {
            add("-demangle")
            addAll(listOf("-object_path_lto", "temporary.o", "-lto_library", libLTO))
            addAll(listOf("-dynamic", "-arch", propertyTargetString("arch")))
            addAll(osVersionMin)
            addAll(listOf("-syslibroot", targetSysRoot, "-o", executable))
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerDebugFlags)
            addAll(linkerKonanFlags)
            add("-lSystem")
        }
    }

    open fun dsymutilCommand(executable: ExecutableFile): List<String> = listOf(dsymutil, executable)

    open fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> =
            listOf(dsymutil, "-dump-debug-map" ,executable)
}

internal open class LinuxBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val llvmLib = distribution.llvmLib
    private val libGcc = "$targetSysRoot/${propertyTargetString("libGcc")}"
    private val linker = "$targetToolchain/bin/ld.gold"
    private val pluginOptimizationFlags = propertyTargetList("pluginOptimizationFlags")
    private val specificLibs
        = propertyTargetList("abiSpecificLibraries").map{it -> "-L${targetSysRoot}/$it"}

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String> {
        // TODO: Can we extract more to the konan.properties?
        return mutableListOf(linker).apply {
            addAll(listOf("--sysroot=${targetSysRoot}",
                    "-export-dynamic", "-z", "relro", "--hash-style=gnu",
                    "--build-id", "--eh-frame-hdr", // "-m", "elf_x86_64",
                    "-dynamic-linker", propertyTargetString("dynamicLinker"),
                    "-o", executable,
                    "${targetSysRoot}/usr/lib64/crt1.o",
                    "${targetSysRoot}/usr/lib64/crti.o", "${libGcc}/crtbegin.o",
                    "-L${llvmLib}", "-L${libGcc}"))
            addAll(specificLibs)
            addAll(listOf("-L${targetSysRoot}/../lib", "-L${targetSysRoot}/lib", "-L${targetSysRoot}/usr/lib"))
            if (optimize) addAll(listOf("-plugin", "$llvmLib/LLVMgold.so") + pluginOptimizationFlags)
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerDebugFlags)
            addAll(linkerKonanFlags)
            addAll(listOf("-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
                    "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
                    "${libGcc}/crtend.o",
                    "${targetSysRoot}/usr/lib64/crtn.o"))
        }
    }
}

internal open class MingwPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val linker = "$targetToolchain/bin/clang++"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String> {
        return mutableListOf(linker).apply {
            addAll(listOf("-o", executable))
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerDebugFlags)
        }
    }

    override fun linkCommandSuffix() = linkerKonanFlags
}

internal open class WasmPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val clang = "clang"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean): List<String> {

        //No link stage for WASM yet, just give '.wasm' as output.
        return mutableListOf("/bin/cp", objectFiles.single(), executable)
    }
}

internal class LinkStage(val context: Context) {

    val config = context.config.configuration
    val target = context.config.targetManager.target

    private val distribution = context.config.distribution

    private val platform = when (target) {
        KonanTarget.LINUX, KonanTarget.RASPBERRYPI ->
            LinuxBasedPlatform(distribution)
        KonanTarget.MACBOOK, KonanTarget.IPHONE, KonanTarget.IPHONE_SIM ->
            MacOSBasedPlatform(distribution)
        KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64 ->
            AndroidPlatform(distribution)
        KonanTarget.MINGW ->
            MingwPlatform(distribution)
        KonanTarget.WASM32 ->
            WasmPlatform(distribution)
    }

    private val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    private val debug = config.get(KonanConfigKeys.DEBUG) ?: false
    private val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    private val emitted = context.bitcodeFileName
    private val libraries = context.config.libraries

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { !it.isEmpty() })
    }

    private fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        val combined = temporary("combined", ".o")

        val tool = distribution.llvmLto
        val command = mutableListOf(tool, "-o", combined)
        command.addNonEmpty(platform.llvmLtoFlags)
        when {
            optimize -> command.addNonEmpty(platform.llvmLtoOptFlags)
            debug    -> command.addNonEmpty(platform.llvmDebugOptFlags)
            else     -> command.addNonEmpty(platform.llvmLtoNooptFlags)
        }
        command.addNonEmpty(files)
        runTool(*command.toTypedArray())

        return combined
    }

    private fun temporary(name: String, suffix: String): String {
        val temporaryFile = createTempFile(name, suffix)
        temporaryFile.deleteOnExit()
        return temporaryFile.absolutePath
    }

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.targetToolchain}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, args: List<String>) {
        val absoluteToolName = "${distribution.llvmBin}/$tool"
        val command = listOf(absoluteToolName) + args
        runTool(*command.toTypedArray())
    }

    private fun bitcodeToWasm(bitcodeFiles: List<BitcodeFile>): String {
        val combinedBc = temporary("combined", ".bc")
        hostLlvmTool("llvm-link", bitcodeFiles + listOf("-o", combinedBc))

        val combinedS = temporary("combined", ".s")
        targetTool("llc", combinedBc, "-o", combinedS)

        val s2wasmFlags = platform.s2wasmFlags.toTypedArray()
        val combinedWast = temporary( "combined", ".wast")
        targetTool("s2wasm", combinedS, "-o", combinedWast, *s2wasmFlags)

        val combinedWasm = temporary( "combined", ".wasm")
        val combinedSmap = temporary( "combined", ".smap")
        targetTool("wasm-as", combinedWast, "-o", combinedWasm, "-g", "-s", combinedSmap)

        return combinedWasm
    }

    private fun asLinkerArgs(args: List<String>): List<String> {
        if (platform.useCompilerDriverAsLinker) {
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
        get() = if (nomain) emptyList() else platform.entrySelector

    private fun link(objectFiles: List<ObjectFile>, libraryProvidedLinkerFlags: List<String>): ExecutableFile? {
        val executable = context.config.outputFile

        val linkCommand = platform.linkCommand(objectFiles, executable, optimize, debug) +
                platform.targetLibffi +
                asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                entryPointSelector +
                platform.linkCommandSuffix() +
                libraryProvidedLinkerFlags

        try {
            runTool(*linkCommand.toTypedArray())
        } catch (e: KonanExternalToolFailure) {
            return null
        }
        if (platform is MacOSBasedPlatform && context.shouldContainDebugInfo()) {
            if (context.phase?.verbose ?: false)
                runTool(*platform.dsymutilDryRunVerboseCommand(executable).toTypedArray())
            runTool(*platform.dsymutilCommand(executable).toTypedArray())
        }
        return executable
    }

    private fun executeCommand(vararg command: String): Int {

        context.log{""}
        context.log{command.asList<String>().joinToString(" ")}

        val builder = ProcessBuilder(command.asList())

        // Inherit main process output streams.
        val isDsymUtil = platform is MacOSBasedPlatform && command[0] == platform.dsymutil

        builder.redirectOutput(Redirect.INHERIT)
        builder.redirectInput(Redirect.INHERIT)
        if (!isDsymUtil)
            builder.redirectError(Redirect.INHERIT)


        val process = builder.start()
        if (isDsymUtil) {
            /**
             * llvm-lto has option -alias that lets tool to know which symbol we use instead of _main,
             * llvm-dsym doesn't have such a option, so we ignore annoying warning manually.
             */
            val errorStream = process.errorStream
            val outputStream = bufferedReader(errorStream)
            while (true) {
                val line = outputStream.readLine() ?: break
                if (!line.contains("warning: could not find object file symbol for symbol _main"))
                    System.err.println(line)
            }
            outputStream.close()
        }
        val exitCode =  process.waitFor()
        return exitCode
    }

    private fun runTool(vararg command: String) {
        val code = executeCommand(*command)
        if (code != 0) throw KonanExternalToolFailure("The ${command[0]} command returned non-zero exit code: $code.")
    }

    fun linkStage() {
        context.log{"# Compiler root: ${distribution.konanHome}"}

        val bitcodeFiles = listOf(emitted) +
            libraries.map{it -> it.bitcodePaths}.flatten()

        val libraryProvidedLinkerFlags = 
            libraries.map{it -> it.linkerOpts}.flatten()

        var objectFiles: List<String> = listOf()

        val phaser = PhaseManager(context)
        phaser.phase(KonanPhase.OBJECT_FILES) {
            objectFiles = listOf( 
                if (target == KonanTarget.WASM32)
                    bitcodeToWasm(bitcodeFiles) 
                else 
                    llvmLto(bitcodeFiles)
            )
        }
        phaser.phase(KonanPhase.LINKER) {
            link(objectFiles, libraryProvidedLinkerFlags)
        }
    }
}

