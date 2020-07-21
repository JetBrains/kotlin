/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.TrivialRunParameters
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration

class KonanExternalSystemState(
    private val configuration: BinaryRunConfiguration,
    private val project: Project,
    private val lldbConfiguration: LLDBDriverConfiguration,
    environment: ExecutionEnvironment,
    gradleConfiguration: GradleRunConfiguration
) : ExternalSystemRunnableState(gradleConfiguration.settings, project, true, gradleConfiguration, environment) {

    override fun startDebugProcess(
        session: XDebugSession,
        env: ExecutionEnvironment
    ): XDebugProcess {
        execute(env.executor, env.runner)

        val debugVariant = configuration.executable?.variants
            ?.firstOrNull { it is BinaryExecutable.Variant.Debug }
            ?: error("There is no debug variant!")

        val attachmentStrategy = configuration.attachmentStrategy
            ?: error("There is no attachment strategy!")

        val installer = KonanLLDBInstaller(debugVariant.file, configuration)
        val runParams = TrivialRunParameters(lldbConfiguration, installer)
        val searchScope = GlobalSearchScopes.executionScope(project, env.runProfile)
        val consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope)

        val result = KonanRemoteDebugProcess(
            runParams,
            session,
            consoleBuilder,
            debugVariant.file,
            attachmentStrategy
        )
        result.start()
        return result
    }
}