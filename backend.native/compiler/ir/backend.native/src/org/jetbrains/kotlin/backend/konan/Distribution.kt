package org.jetbrains.kotlin.backend.konan

import java.io.File
import java.util.Properties

public class Distribution(val properties: KonanProperties,
    val os: String, val target: String)  {

    companion object {

        private fun findKonanHome(): String {
            val value = System.getProperty("konan.home", "dist")
            val path = File(value).absolutePath 
            return path
        }

        val konanHome = findKonanHome()
        val propertyFile = "$konanHome/konan/konan.properties"

        val lib = "$konanHome/lib"

        // TODO: Pack all needed things to dist.
        val dependencies = "$konanHome/../dependencies/all" 

        val start = "$lib/start.kt.bc"
        val stdlib = "$lib/stdlib.kt.bc"
        val runtime = "$lib/runtime.bc"
        val launcher = "$lib/launcher.bc"
    }

    val llvmHome = "$dependencies/${properties.propertyString("llvmHome.$os")}" 
    val sysRoot = "$dependencies/${properties.propertyString("sysRoot.$os")}"
    val libGcc = "$dependencies/${properties.propertyString("libGcc.$os")}" 

    val llvmBin = "$llvmHome/bin"
    val llvmLib = "$llvmHome/lib"

    val llvmOpt = "$llvmBin/opt"
    val llvmLlc = "$llvmBin/llc"
    val llvmLto = "$llvmBin/llvm-lto"
    val llvmLink = "$llvmBin/llvm-link"
    val libCppAbi = "$llvmLib/libc++abi.a"
    val libLTO = when (os) {
        "osx" -> "$llvmLib/libLTO.dylib" 
        "linux" -> "$llvmLib/libLTO.so" 
        else -> error("Don't know libLTO location for the platform.")
    }
}
