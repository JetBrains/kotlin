/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession

class KonanCommandLineState(environment: ExecutionEnvironment, val launcher: IdeaKonanLauncher) : CommandLineState(environment) {

    @Throws(ExecutionException::class)
    fun startDebugProcess(session: XDebugSession): XDebugProcess {
        return launcher.startDebugProcess(this, session)
    }

    @Throws(ExecutionException::class)
    override fun startProcess(): ProcessHandler {
        return launcher.startProcess(this)
    }
}