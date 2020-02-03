/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.PropertiesUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.Version
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.EnvironmentUtil
import com.intellij.util.SystemProperties
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Service
class AndroidToolkit(project: Project) {
    val home: File? = EnvironmentUtil.getValue("ANDROID_SDK_ROOT")?.let { File(it) }
        ?: readHomeFromLocalProperties(project)

    val adb: File? = home?.let { File(File(it, "platform-tools"), "adb".exe) }
    val emulator: File? = home?.let { File(File(it, "emulator"), "emulator".exe) }
    val buildTools: File? = home?.let { home ->
        File(home, "build-tools").listFiles()?.maxBy {
            Version.parseVersion(it.name) ?: Version(0, 0, 0)
        }
    }
    val aapt: File? = buildTools?.let { File(it, "aapt".exe) }

    val avdData: File = File(File(SystemProperties.getUserHome(), ".android"), "avd")

    private val String.exe
        get() =
            if (SystemInfo.isWindows) "$this.exe"
            else this

    private fun readHomeFromLocalProperties(project: Project): File? {
        val basePath = project.basePath?.let { File(FileUtil.toCanonicalPath(it)) } ?: return null
        val propertiesFile = File(basePath, "local.properties")
        if (!propertiesFile.exists() || propertiesFile.isDirectory) return null
        return try {
            Files.newBufferedReader(propertiesFile.toPath()).use { fis ->
                PropertiesUtil.loadProperties(fis)["sdk.dir"]?.let { File(it) }
            }
        } catch (e: IOException) {
            null
        }
    }

    companion object {
        fun getInstance(project: Project): AndroidToolkit = project.service()
    }
}