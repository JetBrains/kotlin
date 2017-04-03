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

import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

class Distribution(val config: CompilerConfiguration) {

    val targetManager = TargetManager(config)
    val target = 
        if (!targetManager.crossCompile) "host" 
        else targetManager.current.name.toLowerCase()
    val suffix = targetManager.currentSuffix()
    val hostSuffix = TargetManager.host.suffix
    init { if (!targetManager.crossCompile) assert(suffix == hostSuffix) }

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

    val dependenciesDir = "$konanHome/dependencies"
    val dependencies = properties.propertyList("dependencies.$suffix")

    val stdlib = "$lib/stdlib.kt.bc"
    val start = "$lib/start.kt.bc"
    val launcher = "$lib/launcher.bc"
    val runtime = config.get(KonanConfigKeys.RUNTIME_FILE) 
        ?: "$lib/runtime.bc"

    val llvmHome = "$dependenciesDir/${properties.propertyString("llvmHome.$hostSuffix")}"
    val sysRoot = "$dependenciesDir/${properties.propertyString("sysRoot.$hostSuffix")}"

    val libGcc = "$dependenciesDir/${properties.propertyString("libGcc.$suffix")}"
    val targetSysRoot = if (properties.hasProperty("targetSysRoot.$suffix")) {
            "$dependenciesDir/${properties.propertyString("targetSysRoot.$suffix")}"
        } else {
            sysRoot
        }

    val libffi = "$dependenciesDir/${properties.propertyString("libffiDir.$suffix")}/lib/libffi.a"

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
