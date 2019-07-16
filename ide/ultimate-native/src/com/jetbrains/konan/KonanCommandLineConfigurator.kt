/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.SimpleProgramParameters
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.cidr.execution.CidrExecUtil
import com.jetbrains.cidr.execution.EnvParameterNames
import java.io.File

class KonanCommandLineConfigurator(private val appParameters: SimpleProgramParameters) {

    private val workDirectory: File?
        get() {
            val result = appParameters.workingDirectory
            return if (StringUtil.isEmptyOrSpaces(result)) null else File(result)
        }

    fun configureCommandLine(cl: GeneralCommandLine) {
        cl.charset = Charsets.UTF_8
        cl.workDirectory = workDirectory
        cl.parametersList.addAll(appParameters.programParametersList.list)

        val env = cl.environment
        env.putAll(appParameters.env)
        if (SystemInfo.isMac) CidrExecUtil.setIfAbsent(env, EnvParameterNames.NS_UNBUFFERED_IO, EnvParameterNames.YES)

        cl.withParentEnvironmentType(
            if (appParameters.isPassParentEnvs)
                GeneralCommandLine.ParentEnvironmentType.CONSOLE
            else
                GeneralCommandLine.ParentEnvironmentType.NONE
        )
    }
}