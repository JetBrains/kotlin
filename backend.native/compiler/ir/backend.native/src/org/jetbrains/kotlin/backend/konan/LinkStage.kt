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

    abstract val llvmLtoFlags: List<String>
    abstract val llvmLlcFlags: List<String>

    abstract val linker: String 
    abstract val linkerKonanFlags: List<String>
    abstract val linkerOptimizationFlags: List<String>

    abstract fun linkCommand(objectFiles: List<ObjectFile>, 
        executable: ExecutableFile, optimize: Boolean): List<String>
}


internal open class MacOSPlatform(distribution: Distribution) 
    : PlatformFlags(distribution) {

    override val llvmLtoFlags = properties.propertyList("llvmLtoFlags.osx")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.osx")

    override val linkerOptimizationFlags = 
        properties.propertyList("linkerOptimizationFlags.osx")
    override val linkerKonanFlags = properties.propertyList("linkerKonanFlags.osx")
    override val linker = "${distribution.sysRoot}/usr/bin/ld" 

    open val arch = properties.propertyString("arch.osx")!!
    open val osVersionMin = properties.propertyList("osVersionMin.osx")

    open val sysRoot = distribution.sysRoot
    open val targetSysRoot = sysRoot
    open val llvmLib = distribution.llvmLib

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {

        return mutableListOf<String>(linker, "-demangle") +
            if (optimize) listOf("-object_path_lto", "temporary.o", "-lto_library", distribution.libLTO) else {listOf<String>()} +
            listOf( "-dynamic", "-arch", arch) +
            osVersionMin + 
            listOf("-syslibroot", "$targetSysRoot", 
            "-o", executable) +
            objectFiles + 
            if (optimize) linkerOptimizationFlags else {listOf<String>()} +
            linkerKonanFlags +
            listOf("-lSystem")
    }
}

internal class IPhoneOSfromMacOSPlatform(distribution: Distribution) 
    : MacOSPlatform(distribution) {

    override val arch = properties.propertyString("arch.osx-ios")!!
    override val osVersionMin = properties.propertyList("osVersionMin.osx-ios")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.osx-ios")
    override val targetSysRoot = "${distribution.dependencies}/${properties.propertyString("targetSysRoot.osx-ios")!!}"
}

internal class IPhoneSimulatorFromMacOSPlatform(distribution: Distribution) 
    : MacOSPlatform(distribution) {

    override val arch = properties.propertyString("arch.osx-ios-sim")!!
    override val osVersionMin = properties.propertyList("osVersionMin.osx-ios-sim")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.osx-ios-sim")
    override val targetSysRoot = "${distribution.dependencies}/${properties.propertyString("targetSysRoot.osx-ios-sim")!!}"
}
internal class LinuxPlatform(distribution: Distribution) 
    : PlatformFlags(distribution) {

    override val llvmLtoFlags = properties.propertyList("llvmLtoFlags.linux")
    override val llvmLlcFlags = properties.propertyList("llvmLlcFlags.linux")

    override val linkerOptimizationFlags = 
        properties.propertyList("linkerOptimizationFlags.linux")
    override val linkerKonanFlags = properties.propertyList("linkerKonanFlags.linux")
    override val linker = "${distribution.sysRoot}/../bin/ld.gold" 

    val pluginOptimizationFlags = 
        properties.propertyList("pluginOptimizationFlags.linux")

    override fun linkCommand(objectFiles: List<String>, executable: String, optimize: Boolean): List<String> {

        val sysRoot = distribution.sysRoot
        val llvmLib = distribution.llvmLib
        val libGcc = distribution.libGcc

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

    val config = context.config.configuration

    val targetManager = TargetManager(config)
    private val distribution = 
        Distribution(context.config.configuration)
    private val properties = distribution.properties

    val platform: PlatformFlags = when (TargetManager.host) {
        KonanTarget.LINUX -> LinuxPlatform(distribution)
        KonanTarget.MACBOOK -> when (targetManager.current) {
            KonanTarget.IPHONE_SIM
                -> IPhoneSimulatorFromMacOSPlatform(distribution)
            KonanTarget.IPHONE 
                -> IPhoneOSfromMacOSPlatform(distribution)
            KonanTarget.MACBOOK
                -> MacOSPlatform(distribution)
            else -> TODO("Target not implemented yet.")
        }
        else -> TODO("Target not implemented yet")
    }
    val suffix = targetManager.currentSuffix()

    val optimize = config.get(KonanConfigKeys.OPTIMIZATION) ?: false
    val emitted = config.get(KonanConfigKeys.BITCODE_FILE)!!
    val nostdlib = config.get(KonanConfigKeys.NOSTDLIB) ?: false
    val libraries = context.config.libraries

    fun llvmLto(files: List<BitcodeFile>): ObjectFile {
        val tmpCombined = File.createTempFile("combined", ".o")
        tmpCombined.deleteOnExit()
        val combined = tmpCombined.absolutePath

        val tool = distribution.llvmLto
        val command = mutableListOf(tool, "-o", combined)
        if (optimize) {
            command.addAll(platform.llvmLtoFlags)
        }
        command.addAll(files)
        runTool(*command.toTypedArray())

        return combined
    }

    fun llvmLlc(file: BitcodeFile): ObjectFile {
        val tmpObjectFile = File.createTempFile(File(file).name, ".o")
        tmpObjectFile.deleteOnExit()
        val objectFile = tmpObjectFile.absolutePath

        val tool = distribution.llvmLlc
        val command = listOf(distribution.llvmLlc, "-o", objectFile, "-filetype=obj") +
                properties.propertyList("llvmLlcFlags.$suffix") + listOf(file)
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
        context.log("# Compiler root: ${distribution.konanHome}")

        val bitcodeFiles = listOf<BitcodeFile>(emitted, distribution.start, distribution.runtime, 
            distribution.launcher) + libraries

        val objectFiles = if (optimize) {
            listOf( llvmLto(bitcodeFiles ) )
        } else {
            bitcodeFiles.map{ it -> llvmLlc(it) }
        }

        val executable = link(objectFiles)
    }
}

