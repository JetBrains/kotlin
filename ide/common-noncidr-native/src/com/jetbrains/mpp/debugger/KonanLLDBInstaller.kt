/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.cidr.execution.CidrExecUtil
import com.jetbrains.cidr.execution.EnvParameterNames
import com.jetbrains.cidr.execution.Installer
import com.jetbrains.mpp.BinaryRunConfigurationBase
import java.io.File

class KonanLLDBInstaller(
    private val executableFile: File,
    private val configuration: BinaryRunConfigurationBase
) : Installer {
    override fun getExecutableFile(): File = executableFile

    override fun getAppWorkingDir(): File? = executableFile.parentFile

    override fun install(): GeneralCommandLine {
        val result = GeneralCommandLine()
        configureCommandLine(result)
        return result
    }

    fun configureCommandLine(cl: GeneralCommandLine) {
        val programParameters = SimpleProgramParameters()

        object : ProgramParametersConfigurator() {
            override fun getDefaultWorkingDir(project: Project) = executableFile.parent
        }.configureConfiguration(programParameters, configuration)

        cl.exePath = executableFile.toString()
        cl.charset = Charsets.UTF_8
        cl.workDirectory = File(programParameters.workingDirectory)
        cl.parametersList.addAll(programParameters.programParametersList.list)

        val env = cl.environment
        env.putAll(programParameters.env)
        if (SystemInfo.isMac) CidrExecUtil.setIfAbsent(env, EnvParameterNames.NS_UNBUFFERED_IO, EnvParameterNames.YES)

        cl.withParentEnvironmentType(
            if (programParameters.isPassParentEnvs)
                GeneralCommandLine.ParentEnvironmentType.CONSOLE
            else
                GeneralCommandLine.ParentEnvironmentType.NONE
        )
    }
}