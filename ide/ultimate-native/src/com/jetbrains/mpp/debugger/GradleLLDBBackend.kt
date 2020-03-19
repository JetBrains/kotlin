/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.MPPBinaryRunConfiguration
import com.jetbrains.mpp.MPPBinaryRunConfigurationType
import com.jetbrains.mpp.MPPWorkspace
import com.jetbrains.mpp.KonanExecutable

class GradleLLDBBackend : LLDBBackendBase() {

    override fun binaryConfiguration(runManager: RunManager, konanExecutable: KonanExecutable): MPPBinaryRunConfiguration {
        val result = runManager.allSettings.firstOrNull {
            (it.configuration as? MPPBinaryRunConfiguration)?.executable == konanExecutable
        } ?: throw ExecutionException("No configuration for executable=${konanExecutable.base}")

        return result.configuration as MPPBinaryRunConfiguration
    }

    override fun findExecutable(project: Project, processName: String): KonanExecutable? {
        val taskName = processName.substring(processName.lastIndexOf(':') + 1)
        val projectPrefix = processName.substring(0, processName.lastIndexOf(':') + 1)

        val workspace = MPPWorkspace.getInstance(project)

        if (taskName.startsWith("run")) {
            val executableId = taskName.removePrefix("run")
            return workspace.executables.find {
                it.base.projectPrefix == projectPrefix &&
                        it.executionTargets.any { t -> t.gradleTask.contains(executableId) }
            }
        }

        if (taskName.endsWith("Test")) {
            val targetId = taskName.removeSuffix("Test")
            return workspace.executables.find {
                it.base.projectPrefix.endsWith(projectPrefix) &&
                        it.base.name.contains(targetId) && it.base.name.contains("test")
            }
        }

        return null
    }

    override fun runSettings(runManager: RunManager, processName: String) =
        runManager.createConfiguration(processName, MPPBinaryRunConfigurationType.instance.factory)
}