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

import java.io.File
import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
internal abstract class PlatformFlags(val distribution: Distribution) {
    val properties = distribution.properties

    val hostSuffix = distribution.hostTargetSuffix
    val targetSuffix = distribution.targetSuffix

    open val llvmLtoNooptFlags 
        = propertyTargetList("llvmLtoNooptFlags")
    open val llvmLtoOptFlags
        = propertyTargetList("llvmLtoOptFlags")
    open val llvmLtoFlags 
        = propertyTargetList("llvmLtoFlags")
    open val entrySelector 
        = propertyTargetList("entrySelector")
    open val linkerOptimizationFlags 
        = propertyTargetList("linkerOptimizationFlags")
    open val linkerKonanFlags 
        = propertyTargetList("linkerKonanFlags")

   abstract fun linkCommand(objectFiles: List<ObjectFile>,
        executable: ExecutableFile, optimize: Boolean): List<String>

    protected fun propertyHostString(name: String)
        = properties.propertyString(name, hostSuffix)!!
    protected fun propertyHostList(name: String)
        = properties.propertyList(name, hostSuffix)
    protected fun propertyTargetString(name: String)
        = properties.propertyString(name, targetSuffix)!!
    protected fun propertyTargetList(name: String) 
        = properties.propertyList(name, targetSuffix)
}


internal open class AndroidPlatform(distribution: Distribution)
    : PlatformFlags(distribution) {

    private val prefix = "${distribution.targetToolchain}/bin/"
    private val clang = "$prefix/clang"

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {
        return mutableListOf<String>(clang, "-o", executable, "-fPIC", "-shared") +
                    objectFiles +
                    (if (optimize) linkerOptimizationFlags else emptyList()) +
                linkerKonanFlags
    }
}

internal open class MacOSBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution) {

    // TODO: move 'ld' out of the host sysroot, as it doesn't belong here.
    private val linker = "${distribution.hostSysRoot}/usr/bin/ld"
    private val dsymutil = "${distribution.llvmBin}/llvm-dsymutil"

    open val osVersionMin by lazy {
        listOf(
                propertyTargetString("osVersionMinFlagLd"),
                propertyTargetString("osVersionMin") + ".0")
    }

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {
        return mutableListOf(linker, "-demangle") +
                    listOf("-object_path_lto", "temporary.o", "-lto_library", distribution.libLTO) +
                    listOf("-dynamic", "-arch", propertyTargetString("arch")) +
                    osVersionMin +
                    listOf("-syslibroot", distribution.targetSysRoot, "-o", executable) +
                    objectFiles +
                    (if (optimize) linkerOptimizationFlags else emptyList()) +
                    linkerKonanFlags + listOf("-lSystem")
    }

    open fun dsymutilCommand(executable: ExecutableFile): List<String> {
        return listOf(dsymutil, executable)
    }

    open fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> {
        return listOf(dsymutil, "-dump-debug-map" ,executable)
    }
}

internal open class LinuxBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution) {

    val targetSysRoot = distribution.targetSysRoot
    val llvmLib = distribution.llvmLib
    val libGcc = "$targetSysRoot/${propertyTargetString("libGcc")!!}"
    val linker = "${distribution.targetToolchain}/bin/ld.gold"
    val pluginOptimizationFlags = propertyTargetList("pluginOptimizationFlags")
    val specificLibs
        = propertyTargetList("abiSpecificLibraries").map{it -> "-L${targetSysRoot}/$it"}

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {
        // TODO: Can we extract more to the konan.properties?
        return mutableListOf(linker,
            "--sysroot=${targetSysRoot}",
            "-export-dynamic", "-z", "relro", "--hash-style=gnu", 
            "--build-id", "--eh-frame-hdr", // "-m", "elf_x86_64",
            "-dynamic-linker", propertyTargetString("dynamicLinker"),
            "-o", executable,
            "${targetSysRoot}/usr/lib64/crt1.o", "${targetSysRoot}/usr/lib64/crti.o", "${libGcc}/crtbegin.o",
            "-L${llvmLib}", "-L${libGcc}") +
            specificLibs +
            listOf("-L${targetSysRoot}/../lib", "-L${targetSysRoot}/lib", "-L${targetSysRoot}/usr/lib") + 
            if (optimize) listOf("-plugin", "$llvmLib/LLVMgold.so") + pluginOptimizationFlags else {listOf<String>()} +
            objectFiles +
            if (optimize) linkerOptimizationFlags else {listOf<String>()} +
            linkerKonanFlags +
            listOf("-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed", 
            "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
            "${libGcc}/crtend.o",
            "${targetSysRoot}/usr/lib64/crtn.o")
    }
}

internal class LinkStage(val context: Context) {

    val config = context.config.configuration

    private val distribution = context.config.distribution

    val platform = when (context.config.targetManager.current) {
        KonanTarget.LINUX, KonanTarget.RASPBERRYPI ->
            LinuxBasedPlatform(distribution)
        KonanTarget.MACBOOK, KonanTarget.IPHONE, KonanTarget.IPHONE_SIM ->
            MacOSBasedPlatform(distribution)
        KonanTarget.ANDROID_ARM32 ->
            AndroidPlatform(distribution)
        else ->
            error("Unexpected target platform: ${context.config.targetManager.current}")
    }

    val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    val nomain = config.get(KonanConfigKeys.NOMAIN) ?: false
    val emitted = context.bitcodeFileName
    val libraries = context.config.libraries

    fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        val tmpCombined = File.createTempFile("combined", ".o")
        tmpCombined.deleteOnExit()
        val combined = tmpCombined.absolutePath

        val tool = distribution.llvmLto
        val command = mutableListOf(tool, "-o", combined)
        command.addAll(platform.llvmLtoFlags)
        if (optimize) {
            command.addAll(platform.llvmLtoOptFlags)
        } else {
            command.addAll(platform.llvmLtoNooptFlags)
        }
        command.addAll(files)
        runTool(*command.toTypedArray())

        return combined
    }

    fun asLinkerArgs(args: List<String>): List<String> {
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
    val entryPointSelector: List<String> 
        get() = if (nomain) emptyList() else platform.entrySelector

    fun link(objectFiles: List<ObjectFile>): ExecutableFile {
        val executable = config.get(KonanConfigKeys.OUTPUT_FILE)!!
        val linkCommand = platform.linkCommand(objectFiles, executable, optimize) +
                distribution.libffi +
                asLinkerArgs(config.getNotNull(KonanConfigKeys.LINKER_ARGS)) +
                entryPointSelector

        runTool(*linkCommand.toTypedArray())
        if (platform is MacOSBasedPlatform && context.shouldContainDebugInfo()) {
            if (context.phase?.verbose ?: false)
                runTool(*platform.dsymutilDryRunVerboseCommand(executable).toTypedArray())
            runTool(*platform.dsymutilCommand(executable).toTypedArray())
        }

        return executable
    }

    fun executeCommand(vararg command: String): Int {

        context.log{""}
        context.log{command.asList<String>().joinToString(" ")}

        val builder = ProcessBuilder(command.asList())

        // Inherit main process output streams.
        builder.redirectOutput(Redirect.INHERIT)
        builder.redirectInput(Redirect.INHERIT)
        builder.redirectError(Redirect.INHERIT)

        val process = builder.start()
        val exitCode =  process.waitFor()
        return exitCode
    }

    fun runTool(vararg command: String) {
        val code = executeCommand(*command)
        if (code != 0) error("The ${command[0]} command returned non-zero exit code: $code.")
    }

    fun linkStage() {
        context.log{"# Compiler root: ${distribution.konanHome}"}

        val bitcodeFiles = listOf(emitted) +
            libraries.map{it -> it.bitcodePaths}.flatten()

        var objectFiles: List<String> = listOf()

        val phaser = PhaseManager(context)
        phaser.phase(KonanPhase.OBJECT_FILES) {
            objectFiles = if (optimize) {
                listOf( llvmLto(bitcodeFiles ) )
            } else {
                listOf( llvmLto(bitcodeFiles ) )
                // Or, alternatively, go through llc bitcode compiler.
                //bitcodeFiles.map{ it -> llvmLlc(it) }
            }
        }
        phaser.phase(KonanPhase.LINKER) {
            link(objectFiles)
        }
    }
}

