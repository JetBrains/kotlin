/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.execution

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.CidrCommandLineConfigurator
import com.jetbrains.cidr.execution.Installer
import java.io.File

class AppleInstaller(
    private val project: Project,
    private val bundle: File,
    private val device: AppleDevice
) : Installer {
    private var alreadyInstalled: GeneralCommandLine? = null

    override fun install(): GeneralCommandLine {
        alreadyInstalled?.let { return it }

        val commandLine = device.install(bundle, project)

        val params = SimpleProgramParameters().also {
            it.workingDirectory = File(commandLine.exePath).parentFile.parent
            it.isPassParentEnvs = false
        }
        CidrCommandLineConfigurator(project, params).configureCommandLine(commandLine)

        alreadyInstalled = commandLine
        return commandLine
    }

    override fun getExecutableFile(): File = bundle
    override fun getAppWorkingDir(): File? = null
}