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

package  org.jetbrains.kotlin.native.interop.tool

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.target.TargetManager
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import org.jetbrains.kotlin.konan.util.visibleName

class ToolConfig(userProvidedTargetName: String?, userProvidedKonanProperties: String?, runnerProvidedKonanHome: String) {

    private val targetManager = TargetManager(userProvidedTargetName)
    private val host = TargetManager.host
    private val target = targetManager.target

    private val konanHome = File(runnerProvidedKonanHome).absolutePath
    private val konanPropertiesFile = userProvidedKonanProperties ?. File() ?: File(konanHome, "konan/konan.properties")
    private val properties = konanPropertiesFile.loadProperties()

    private val dependencies = DependencyProcessor.defaultDependenciesRoot

    private val platform = PlatformManager(properties, dependencies.path).platform(target)

    val substitutions = mapOf<String, String> (
        "target" to target.detailedName,
        "arch" to target.architecture.visibleName)

    fun downloadDependencies() = platform.downloadDependencies()

    val defaultCompilerOpts = 
        platform.clang.targetLibclangArgs.toList()

    val llvmHome = platform.absoluteLlvmHome
    val sysRoot = platform.absoluteTargetSysRoot

    val libclang = when (host) {
        KonanTarget.MINGW -> "$llvmHome/bin/libclang.dll"
        else -> "$llvmHome/lib/${System.mapLibraryName("clang")}"
    }
}
