package org.jetbrains.kotlin.backend.konan

import java.io.File
import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
internal abstract class PlatformFlags(val distrib: Distribution, 
    val properties: KonanProperties) {

    abstract val llvmLtoFlags: List<String>
    abstract val llvmLlcFlags: List<String>

    abstract val linker: String 
    abstract val linkerKonanFlags: List<String>
    abstract val linkerOptimizationFlags: List<String>

    abstract fun linkCommand(objectFiles: List<ObjectFile>, 
        executable: ExecutableFile, optimize: Boolean): List<String>
}


internal class MacOSPlatform(distrib: Distribution, 
    properties: KonanProperties) : PlatformFlags(distrib, properties) {

    override val llvmLtoFlags = properties.propertyList("llvmLtoFlags.osx")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.osx")

    override val linkerOptimizationFlags = 
        properties.propertyList("linkerOptimizationFlags.osx")
    override val linkerKonanFlags = properties.propertyList("linkerKonanFlags.osx")
    override val linker = "${distrib.sysRoot}/usr/bin/ld" 

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {

        val sysRoot = distrib.sysRoot
        val llvmLib = distrib.llvmLib
        
        return mutableListOf<String>(linker, "-demangle") +
            if (optimize) listOf("-object_path_lto", "temporary.o", "-lto_library", distrib.libLTO) else {listOf<String>()} +
            listOf( "-dynamic", "-arch", "x86_64", "-macosx_version_min") +
            properties.propertyList("macosVersionMin.osx") + 
            listOf("-syslibroot", "$sysRoot", 
            "-o", executable) +
            objectFiles + 
            if (optimize) linkerOptimizationFlags else {listOf<String>()} +
            linkerKonanFlags +
            listOf("-lSystem", "$llvmLib/clang/3.8.0/lib/darwin/libclang_rt.osx.a")
    }
}

internal class LinuxPlatform(distrib: Distribution, 
    properties: KonanProperties) : PlatformFlags(distrib, properties) {

    override val llvmLtoFlags = properties.propertyList("llvmLtoFlags.linux")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.linux")

    override val linkerOptimizationFlags = 
        properties.propertyList("linkerOptimizationFlags.linux")
    override val linkerKonanFlags = properties.propertyList("linkerKonanFlags.linux")
    override val linker = "${distrib.sysRoot}/../bin/ld.gold" 

    val pluginOptimizationFlags = 
        properties.propertyList("pluginOptimizationFlags.linux")

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {

        val sysRoot = distrib.sysRoot
        val llvmLib = distrib.llvmLib
        val libGcc = distrib.libGcc

        // TODO: Can we extract more to the konan.properties?
        return mutableListOf<String>("$linker",
            "--sysroot=${sysRoot}",
            "-export-dynamic", "-z", "relro", "--hash-style=gnu", 
            "--build-id", "--eh-frame-hdr", "-m", "elf_x86_64",
            "-dynamic-linker", "/lib64/ld-linux-x86-64.so.2",
            "-o", executable,
            "${sysRoot}/usr/lib64/crt1.o", "${sysRoot}/usr/lib64/crti.o", "${libGcc}/crtbegin.o",
            "-L${llvmLib}", "-L${libGcc}", "-L${sysRoot}/../lib64", "-L${sysRoot}/lib64",
            "-L${sysRoot}/usr/lib64", "-L${sysRoot}/../lib", "-L${sysRoot}/lib", "-L${sysRoot}/usr/lib") + 
            if (optimize) listOf("-plugin", "$llvmLib/LLVMgold.so") + pluginOptimizationFlags else {listOf<String>()} +
            objectFiles +
            if (optimize) linkerOptimizationFlags else {listOf<String>()} +
            linkerKonanFlags +
            listOf("-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed", 
            "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
            "${libGcc}/crtend.o",
            "${sysRoot}/usr/lib64/crtn.o")
    }
}


internal class LinkStage(val context: Context) {
    
    private val javaOsName = System.getProperty("os.name")
    private val javaArch = System.getProperty("os.arch")
  
    val os = when (javaOsName) {
        "Mac OS X" -> "osx"
        "Linux" -> "linux"
        else -> error("Unknown operating system: ${javaOsName}") 
    }
    val arch = when (javaArch) {
        "x86_64" -> "x86_64"
        "amd64" -> "x86_64"
        else -> error("Unknown hardware platform: ${javaArch}")
    }

    private val properties = KonanProperties(context.config.configuration)
    private val distrib = Distribution(properties, os, arch)

    val platform: PlatformFlags = when (os) {
        "linux" -> LinuxPlatform(distrib, properties)
        "osx" -> MacOSPlatform(distrib, properties)
        else -> error("Could not tell the current platform")
    }

    val config = context.config.configuration
    val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    val emitted = config.get(KonanConfigKeys.BITCODE_FILE)!!
    val nostdlib = config.get(KonanConfigKeys.NOSTDLIB) ?: false
    val libraries = context.config.libraries

    fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        // TODO: Make it a temporary file.
        val combined = "combined.o" 

        val tool = distrib.llvmLto
        val command = mutableListOf(tool, "-o", combined)
        if (optimize) {
            command.addAll(platform.llvmLtoFlags)
        }
        command.addAll(files)
        runTool(*command.toTypedArray())

        return combined
    }

    fun llvmLlc(file: BitcodeFile): ObjectFile {
        // TODO: Make it a temporary file.
        val objectFile = "$file.o"

        val tool = distrib.llvmLlc
        val command = listOf(distrib.llvmLlc, "-o", objectFile, "-filetype=obj") +  
            properties.propertyList("llvmLlcFlags.$os") + listOf(file)
        runTool(*command.toTypedArray())

        return objectFile
    }

    fun link(objectFiles: List<ObjectFile>): ExecutableFile {
        val executable = config.get(KonanConfigKeys.EXECUTABLE_FILE)!!

        val linkCommand = platform.linkCommand(objectFiles, executable, optimize)
        runTool(*linkCommand.toTypedArray())

        return executable
    }

    fun executeCommand(vararg command: String): Int {

        context.log("")
        context.log(command.asList<String>().joinToString(" "))

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
        context.log("# Compiler root: ${Distribution.konanHome}")

        val bitcodeFiles = listOf<BitcodeFile>(emitted, Distribution.start, Distribution.runtime, 
            Distribution.launcher) + libraries

        val objectFiles = if (optimize) {
            listOf( llvmLto(bitcodeFiles ) )
        } else {
            bitcodeFiles.map{ it -> llvmLlc(it) }
        }

        val executable = link(objectFiles)
    }
}

