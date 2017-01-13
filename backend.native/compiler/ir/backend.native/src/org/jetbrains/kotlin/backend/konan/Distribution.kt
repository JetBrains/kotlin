package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.util.Properties

public class Distribution(val config: CompilerConfiguration) {

    val targetManager = TargetManager(config)
    val target = 
        if (!targetManager.crossCompile) "host" 
        else targetManager.current.name.toLowerCase()
    val suffix = targetManager.currentSuffix()

    private fun findKonanHome(): String {
        val value = System.getProperty("konan.home", "dist")
        val path = File(value).absolutePath 
        return path
    }

    val konanHome = findKonanHome()
    val propertyFile = config.get(KonanConfigKeys.PROPERTY_FILE) 
        ?: "$konanHome/konan/konan.properties"
    val properties = KonanProperties(propertyFile)

    val lib = "$konanHome/lib/$target"

    // TODO: Pack all needed things to dist.
    val dependencies = "$konanHome/../dependencies/all" 

    val stdlib = "$lib/stdlib.kt.bc"
    val start = "$lib/start.kt.bc"
    val launcher = "$lib/launcher.bc"
    val runtime = config.get(KonanConfigKeys.RUNTIME_FILE) 
        ?: "$lib/runtime.bc"

    val llvmHome = "$dependencies/${properties.propertyString("llvmHome.$suffix")}" 
    val sysRoot = "$dependencies/${properties.propertyString("sysRoot.$suffix")}"
    val libGcc = "$dependencies/${properties.propertyString("libGcc.$suffix")}" 

    val llvmBin = "$llvmHome/bin"
    val llvmLib = "$llvmHome/lib"

    val llvmOpt = "$llvmBin/opt"
    val llvmLlc = "$llvmBin/llc"
    val llvmLto = "$llvmBin/llvm-lto"
    val llvmLink = "$llvmBin/llvm-link"
    val libCppAbi = "$llvmLib/libc++abi.a"
    val libLTO = when (TargetManager.host) {
        KonanTarget.MACBOOK -> "$llvmLib/libLTO.dylib" 
        KonanTarget.LINUX -> "$llvmLib/libLTO.so" 
        else -> error("Don't know libLTO location for this platform.")
    }
}
