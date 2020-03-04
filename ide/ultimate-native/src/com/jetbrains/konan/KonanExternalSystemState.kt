/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.ExecutionException
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.konan.debugger.KonanLLDBDriverConfiguration
import com.jetbrains.konan.debugger.KonanRemoteDebugProcess
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class KonanExternalSystemState(
    val configuration: IdeaKonanRunConfiguration,
    private val project: Project,
    environment: ExecutionEnvironment,
    gradleConfiguration: GradleRunConfiguration
) :
    ExternalSystemRunnableState(gradleConfiguration.settings, project, true, gradleConfiguration, environment) {

    override fun startDebugProcess(session: XDebugSession, env: ExecutionEnvironment): XDebugProcess {
        execute(env.executor, env.runner)

        val executableFile = configuration.executable!!.executionTargets.first { it.isDebug }.productFile
        val lldbHome = IdeaKonanWorkspace.getInstance(project).lldbHome
            ?: throw ExecutionException("Debug is impossible without lldbHome")
        val installer = KonanLLDBInstaller(executableFile, configuration)
        val lldbConfiguration = KonanLLDBDriverConfiguration(lldbHome)

        val searchScope = GlobalSearchScopes.executionScope(project, env.runProfile)
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope)

        val result = KonanRemoteDebugProcess(
            TrivialRunParameters(lldbConfiguration, installer),
            session,
            consoleBuilder,
            executableFile,
            configuration.attachmentStrategy!!
        )
        result.start()
        return result
    }
}