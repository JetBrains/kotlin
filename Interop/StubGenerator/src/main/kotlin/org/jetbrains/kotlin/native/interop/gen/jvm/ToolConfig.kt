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

import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.properties.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.DependencyProcessor
import kotlin.reflect.KFunction

class ToolConfig(userProvidedTargetName: String?, userProvidedKonanProperties: String?, runnerProvidedKonanHome: String) {

    private val targetManager = TargetManager(userProvidedTargetName)
    private val host = TargetManager.host
    private val target = targetManager.target

    private val konanHome = File(runnerProvidedKonanHome).absolutePath
    private val konanPropertiesFile = userProvidedKonanProperties ?. File() ?: File(konanHome, "konan/konan.properties")
    private val properties = konanPropertiesFile.loadProperties()

    private val dependencies = DependencyProcessor.defaultDependenciesRoot

    private val targetProperties = KonanProperties(target, properties, dependencies.path)

    val substitutions = mapOf<String, String> (
        "target" to target.detailedName,
        "arch" to target.architecture.userName)

    fun downloadDependencies() = targetProperties.downloadDependencies()

    val llvmHome = targetProperties.absoluteLlvmHome

    val sysRoot get() = targetProperties.absoluteTargetSysRoot

    val defaultCompilerOpts = 
        targetProperties.defaultCompilerOpts()

     val libclang = when (host) {
        KonanTarget.MINGW -> "$llvmHome/bin/libclang.dll"
        else -> "$llvmHome/lib/${System.mapLibraryName("clang")}"
    }
}

private fun maybeExecuteHelper(dependenciesRoot: String, properties: Properties, dependencies: List<String>) {
    try {
        val kClass = Class.forName("org.jetbrains.kotlin.konan.util.Helper0").kotlin
        @Suppress("UNCHECKED_CAST")
        val ctor = kClass.constructors.single() as KFunction<Runnable>
        val result = ctor.call(dependenciesRoot, properties, dependencies)
        result.run()
    } catch (notFound: ClassNotFoundException) {
        // Just ignore, no helper.
    } catch (e: Throwable) {
        throw IllegalStateException("Cannot download dependencies.", e)
    }
}

