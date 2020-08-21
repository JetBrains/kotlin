/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.CidrPathManager
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetSupportException
import java.io.File

class GradleLLDBDriverConfiguration(private val lldbHome: File) : LLDBDriverConfiguration() {

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

    override fun getLLDBFrameworkFile(architecture: ArchitectureType): File = this.lldbHome.resolve(framework)

    override fun getLLDBFrontendFile(architecture: ArchitectureType): File {
        val binaryInPlugin = CidrPathManager.getBinFile(
            GradleLLDBDriverConfiguration::class.java,
            "",
            frontend,
            null
        )

        // TODO: support LLDBFrontend run from different locations
        return when (HostManager.host) {
            KonanTarget.LINUX_X64 -> copyToKonan(binaryInPlugin)
            KonanTarget.MINGW_X64, KonanTarget.MACOS_X64 -> binaryInPlugin
            else -> throw TargetSupportException("Unsupported host target: ${HostManager.host_os()} ${HostManager.host_arch()}")
        }
    }

    override fun useSTLRenderers(): Boolean = false

    private fun copyToKonan(binaryInPlugin: File): File {
        val binaryInKonan = this.lldbHome.resolve("bin/LLDBFrontend")
        binaryInPlugin.copyTo(binaryInKonan, true)
        return binaryInKonan.apply { setExecutable(true) }
    }
}