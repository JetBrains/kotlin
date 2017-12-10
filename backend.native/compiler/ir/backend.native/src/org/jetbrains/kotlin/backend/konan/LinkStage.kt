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
import org.jetbrains.kotlin.backend.konan.util.listConstructor
import org.jetbrains.kotlin.backend.konan.util.If

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
internal abstract class PlatformFlags(val properties: KonanProperties) {
    val llvmLtoNooptFlags = properties.llvmLtoNooptFlags
    val llvmLtoOptFlags = properties.llvmLtoOptFlags
    val llvmLtoFlags = properties.llvmLtoFlags
    val llvmLtoDynamicFlags = properties.llvmLtoDynamicFlags
    val entrySelector = properties.entrySelector
    val linkerOptimizationFlags = properties.linkerOptimizationFlags
    val linkerKonanFlags = properties.linkerKonanFlags
    val linkerNoDebugFlags = properties.linkerNoDebugFlags
    val linkerDynamicFlags = properties.linkerDynamicFlags
    val llvmDebugOptFlags = properties.llvmDebugOptFlags
    val s2wasmFlags = properties.s2wasmFlags
    val targetToolchain = properties.absoluteTargetToolchain
    val targetSysRoot = properties.absoluteTargetSysRoot

    val targetLibffi = properties.libffiDir ?.let { listOf("${properties.absoluteLibffiDir}/lib/libffi.a") } ?: emptyList()

    open val useCompilerDriverAsLinker: Boolean get() = false // TODO: refactor.

    abstract fun linkCommand(objectFiles: List<ObjectFile>,
        executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String>

    open fun linkCommandSuffix(): List<String> = emptyList()

    protected fun propertyTargetString(name: String)
        = properties.targetString(name)!!
    protected fun propertyTargetList(name: String)
        = properties.targetList(name)

    abstract fun filterStaticLibraries(binaries: List<String>): List<String> 

    open fun linkStaticLibraries(binaries: List<String>): List<String> {
        val libraries = filterStaticLibraries(binaries)
        // Let's just pass them as absolute paths
        return libraries
    }

}


internal open class AndroidPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val prefix = "$targetToolchain/bin/"
    private val clang = "$prefix/clang"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String> {
        // liblog.so must be linked in, as we use its functionality in runtime.
        return mutableListOf(clang).apply{
            add("-o")
            add(executable)
            add("-fPIC")
            add("-shared")
            add("-llog")
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerNoDebugFlags)
            if (dynamic) addAll(linkerDynamicFlags)
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
                properties.osVersionMin!! + ".0")
    }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String> {
        return mutableListOf(linker).apply {
            add("-demangle")
            addAll(listOf("-object_path_lto", "temporary.o", "-lto_library", libLTO))
            addAll(listOf("-dynamic", "-arch", propertyTargetString("arch")))
            addAll(osVersionMin)
            addAll(listOf("-syslibroot", targetSysRoot, "-o", executable))
            addAll(objectFiles)
            if (optimize) addAll(linkerOptimizationFlags)
            if (!debug) addAll(linkerNoDebugFlags)
            if (dynamic) addAll(linkerDynamicFlags)
            addAll(linkerKonanFlags)
            add("-lSystem")
        }
    }

    open fun dsymutilCommand(executable: ExecutableFile): List<String> = listOf(dsymutil, executable)

    open fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> =
            listOf(dsymutil, "-dump-debug-map" ,executable)
}
internal open class LinuxBasedPlatform(val distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val llvmLib = distribution.llvmLib
    private val libGcc = "$targetSysRoot/${propertyTargetString("libGcc")}"
    private val linker = "$targetToolchain/bin/ld.gold"
    private val pluginOptimizationFlags = propertyTargetList("pluginOptimizationFlags")
    private val specificLibs
        = propertyTargetList("abiSpecificLibraries").map { "-L${targetSysRoot}/$it" }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String> {
        val isMips = (distribution.target == KonanTarget.LINUX_MIPS32 ||
                      distribution.target == KonanTarget.LINUX_MIPSEL32)
        // TODO: Can we extract more to the konan.properties?
        return listConstructor(
            linker,
            "--sysroot=${targetSysRoot}",
            "-export-dynamic", "-z", "relro",
            "--build-id", "--eh-frame-hdr", // "-m", "elf_x86_64",
            "-dynamic-linker", propertyTargetString("dynamicLinker"),
            "-o", executable,
            If (!dynamic, 
                "${targetSysRoot}/usr/lib64/crt1.o"),
            "${targetSysRoot}/usr/lib64/crti.o", 
            if (dynamic)
                "${libGcc}/crtbeginS.o"
            else
                "${libGcc}/crtbegin.o",
            "-L${llvmLib}", "-L${libGcc}",
            If (!isMips,    // MIPS doesn't support hash-style=gnu
                "--hash-style=gnu"),
            specificLibs,
            "-L${targetSysRoot}/../lib", "-L${targetSysRoot}/lib", "-L${targetSysRoot}/usr/lib",
            If (optimize,
                "-plugin", "$llvmLib/LLVMgold.so",
                pluginOptimizationFlags,
                linkerOptimizationFlags),
            If (!debug, 
                linkerNoDebugFlags),
            If (dynamic, 
                linkerDynamicFlags),
            objectFiles,
            linkerKonanFlags,
            "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
            "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
            if (dynamic)
                "${libGcc}/crtendS.o"
            else
                "${libGcc}/crtend.o",
            "${targetSysRoot}/usr/lib64/crtn.o"
        )
    }
}

internal open class MingwPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val linker = "$targetToolchain/bin/clang++"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isWindowsStaticLib || it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String> {
        return listConstructor(
            linker,
            "-o", executable,
            objectFiles,
            If (optimize, 
                linkerOptimizationFlags),
            If (!debug, 
                linkerNoDebugFlags),
            If (dynamic, 
                linkerDynamicFlags)
        )
    }

    override fun linkCommandSuffix() = linkerKonanFlags
}

internal open class WasmPlatform(distribution: Distribution)
    : PlatformFlags(distribution.targetProperties) {

    private val clang = "clang"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>) 
        = emptyList<String>()

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): List<String> {

        // No link stage for WASM yet, just give '.wasm' as output.
        return mutableListOf(
          *(if (TargetManager.host == KonanTarget.MINGW) arrayOf("${System.getenv("SystemRoot")}/System32/cmd.exe", "/c", "copy") else arrayOf("/bin/cp")),
          objectFiles.single(), executable)
    }
}

internal class LinkStage(val context: Context) {

    val config = context.config.configuration
    val target = context.config.targetManager.target

    private val distribution = context.config.distribution

    private val platform = when (target) {
        KonanTarget.LINUX, KonanTarget.RASPBERRYPI,
        KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 ->
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
    private val dynamic = context.config.produce == CompilerOutputKind.DYNAMIC ||
            context.config.produce == CompilerOutputKind.FRAMEWORK
    private val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    private val emitted = context.bitcodeFileName
    private val libraries = context.llvm.librariesToLink
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
        command.addNonEmpty(platform.llvmLtoDynamicFlags)
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

        val linkCommand = platform.linkCommand(objectFiles, executable, optimize, debug, dynamic) +
                platform.targetLibffi +
                asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                entryPointSelector +
                frameworkLinkerArgs +
                platform.linkCommandSuffix() +
                platform.linkStaticLibraries(includedBinaries) +
                libraryProvidedLinkerFlags

        try {
            runTool(*linkCommand.toTypedArray())
        } catch (e: KonanExternalToolFailure) {
            context.reportCompilationError("linker invocation reported errors")
            return null
        }
        if (platform is MacOSBasedPlatform && context.shouldContainDebugInfo()) {
            if (context.phase?.verbose ?: false)
                runTool(*platform.dsymutilDryRunVerboseCommand(executable).toTypedArray())
            runTool(*platform.dsymutilCommand(executable).toTypedArray())
        }
        if (platform is WasmPlatform) {
            JavaScriptLinker(includedBinaries.filter{it.isJavaScript}, executable)
        }
        return executable
    }

    private fun JavaScriptLinker(jsFiles: List<String>, executable: String): String {
        val linkedJavaScript = File("$executable.js")

        val jsLibsExceptLauncher = jsFiles.filter { it != "launcher.js" }.map { it.removeSuffix(".js") }

        val linkerStub = "var konan = { libraries: [] };\n"

        linkedJavaScript.writeBytes(linkerStub.toByteArray());

        jsFiles.forEach {
            linkedJavaScript.appendBytes(File(it).readBytes())
        }
        return linkedJavaScript.name
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
        val exitCode = process.waitFor()
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

        val includedBinaries = 
            libraries.map{it -> it.includedPaths}.flatten()

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
            link(objectFiles, includedBinaries, libraryProvidedLinkerFlags)
        }
    }
}

