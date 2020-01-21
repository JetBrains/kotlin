/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.io.FileUtil
import java.io.File

class MobileCommandLineState(environment: ExecutionEnvironment, private val xcodeproj: String?) : CommandLineState(environment) {
    override fun startProcess(): ProcessHandler {
        val cl = GeneralCommandLine()
        cl.workDirectory = File(FileUtil.join(environment.project.basePath!!, xcodeproj))
        cl.exePath = "/bin/ls"
        return ColoredProcessHandler(cl)
    }
}