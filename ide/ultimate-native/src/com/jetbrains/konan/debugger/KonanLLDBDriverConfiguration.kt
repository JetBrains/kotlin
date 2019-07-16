/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan.debugger

import com.intellij.execution.ExecutionException
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.CidrPathManager
import com.jetbrains.cidr.execution.debugger.backend.LLDBDriverConfiguration
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetSupportException
import org.jetbrains.kotlin.konan.util.DependencyDirectories
import java.io.File
import java.util.*

class KonanLLDBDriverConfiguration(
    private val konanHome: String
) : LLDBDriverConfiguration() {

    private val dependencyDirFromProperties: String
        get() {
            val propertiesPath = "$konanHome/konan/konan.properties"
            val hostDependenciesKey = "dependencies.${HostManager.host}"

            val propertiesFile = File(propertiesPath).apply {
                if (!exists()) throw ExecutionException("Kotlin/Native properties file is absent at $propertiesPath")
            }

            val hostDependencies = Properties().apply {
                propertiesFile.inputStream().use(::load)
            }.getProperty(hostDependenciesKey) ?: throw ExecutionException("No property $hostDependenciesKey at $propertiesPath")

            val result = hostDependencies.split(" ").firstOrNull { it.startsWith("lldb-") }
                ?: throw ExecutionException("Property $hostDependenciesKey at $propertiesPath does not specify lldb")

            return result
        }

    private val dependencyDir: File = DependencyDirectories.defaultDependenciesRoot.resolve(dependencyDirFromProperties)

    private val framework = when (HostManager.host) {
        KonanTarget.MACOS_X64 -> "LLDB.framework"
        KonanTarget.LINUX_X64 -> "lib/liblldb.so"
        KonanTarget.MINGW_X64 -> "bin/liblldb.dll"
        else -> throw TargetSupportException("Unsupported host target: ${HostManager.host_os()} ${HostManager.host_arch()}")
    }

    private val frontend = when (HostManager.host) {
        KonanTarget.MACOS_X64 -> "macos/LLDBFrontend"
        KonanTarget.LINUX_X64 -> "linux/LLDBFrontend"
        KonanTarget.MINGW_X64 -> "windows/LLDBFrontend.exe"
        else -> throw TargetSupportException("Unsupported host target: ${HostManager.host_os()} ${HostManager.host_arch()}")
    }

    override fun getLLDBFrameworkFile(architecture: ArchitectureType): File = dependencyDir.resolve(framework)

    override fun getLLDBFrontendFile(architecture: ArchitectureType): File {
        val binaryInPlugin = CidrPathManager.getBinFile(
            KonanLLDBDriverConfiguration::class.java,
            "",
            frontend,
            null
        )

        // TODO: support LLDBFrontend run from different locations
        return when(HostManager.host) {
            KonanTarget.LINUX_X64 -> copyToKonan(binaryInPlugin)
            KonanTarget.MINGW_X64, KonanTarget.MACOS_X64 -> binaryInPlugin
            else -> throw TargetSupportException("Unsupported host target: ${HostManager.host_os()} ${HostManager.host_arch()}")
        }
    }

    private fun copyToKonan(binaryInPlugin: File): File {
        val binaryInKonan = dependencyDir.resolve("bin/LLDBFrontend")
        binaryInPlugin.copyTo(binaryInKonan, true)
        return binaryInKonan.apply { setExecutable(true) }
    }
}