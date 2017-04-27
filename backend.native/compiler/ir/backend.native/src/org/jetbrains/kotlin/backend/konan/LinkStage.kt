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

    val hostSuffix = TargetManager.host.suffix
    val targetSuffix = distribution.suffix
    val arch = propertyTargetString("arch")

    open val llvmLtoNooptFlags 
        = propertyHostList("llvmLtoNooptFlags")
    open val llvmLtoOptFlags 
        = propertyHostList("llvmLtoOptFlags")
    open val llvmLtoFlags 
        = propertyHostList("llvmLtoFlags")
    open val entrySelector 
        = propertyHostList("entrySelector")
    open val linkerOptimizationFlags 
        = propertyHostList("linkerOptimizationFlags")
    open val linkerKonanFlags 
        = propertyHostList("linkerKonanFlags")

    abstract val linker: String 

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
    protected fun propertyCommonString(name: String) 
        = properties.propertyString(name, null)!!
    protected fun propertyCommonList(name: String) 
        = properties.propertyList(name, null)
}

internal open class MacOSBasedPlatform(distribution: Distribution)
    : PlatformFlags(distribution) {

    override val linker = "${distribution.sysRoot}/usr/bin/ld"
    private val dsymutil = "${distribution.llvmBin}/llvm-dsymutil"

    open val osVersionMin = listOf(
        propertyTargetString("osVersionMinFlagLd"),
        propertyTargetString("osVersionMin")+".0")
                            
    open val sysRoot = distribution.sysRoot
    open val targetSysRoot = distribution.targetSysRoot

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {

        return mutableListOf<String>(linker, "-demangle") +
            listOf("-object_path_lto", "temporary.o", "-lto_library", distribution.libLTO) +
            listOf( "-dynamic", "-arch", arch) +
            osVersionMin +
            listOf("-syslibroot", "$targetSysRoot",
            "-o", executable) +
            objectFiles + 
            if (optimize) linkerOptimizationFlags else {listOf<String>()} +
            linkerKonanFlags +
            listOf("-lSystem")
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

    open val sysRoot = distribution.sysRoot
    open val targetSysRoot = distribution.targetSysRoot

    val llvmLib = distribution.llvmLib
    val libGcc = distribution.libGcc

    override val linker = "${distribution.sysRoot}/../bin/ld.gold"

    open val pluginOptimizationFlags = 
        propertyHostList("pluginOptimizationFlags")
    open val dynamicLinker = 
        propertyTargetString("dynamicLinker")

    open val specificLibs 
        = propertyTargetList("abiSpecificLibraries").map{it -> "-L${targetSysRoot}/$it"}

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {
        // TODO: Can we extract more to the konan.properties?
        return mutableListOf<String>("$linker",
            "--sysroot=${targetSysRoot}",
            "-export-dynamic", "-z", "relro", "--hash-style=gnu", 
            "--build-id", "--eh-frame-hdr", // "-m", "elf_x86_64",
            "-dynamic-linker", dynamicLinker,
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

    val targetManager = context.config.targetManager
    private val distribution =
        context.config.distribution
    private val properties = distribution.properties

    val platform = when (TargetManager.host) {
        KonanTarget.LINUX ->
            LinuxBasedPlatform(distribution)
        KonanTarget.MACBOOK -> 
            MacOSBasedPlatform(distribution)
        else ->
            error("Unexpected host platform")
    }

    val suffix = targetManager.currentSuffix()

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

    fun llvmLlc(file: BitcodeFile): ObjectFile {
        val tmpObjectFile = File.createTempFile(File(file).name, ".o")
        tmpObjectFile.deleteOnExit()
        val objectFile = tmpObjectFile.absolutePath

        val command = listOf(distribution.llvmLlc, "-o", objectFile, "-filetype=obj") +
                properties.propertyList("llvmLlcFlags.$suffix") + listOf(file)
        runTool(*command.toTypedArray())

        return objectFile
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
        get() = if (nomain) listOf() 
                else platform.entrySelector

    fun link(objectFiles: List<ObjectFile>): ExecutableFile {
        val executable = config.get(KonanConfigKeys.EXECUTABLE_FILE)!!
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

        val bitcodeFiles = listOf<BitcodeFile>(emitted) + 
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

