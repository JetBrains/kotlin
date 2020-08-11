/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.debugger

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.externalSystem.debugger.DebuggerBackendExtension
import com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.runconfig.AttachmentStrategy
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import com.jetbrains.mpp.workspace.WorkspaceBase

abstract class LLDBBackendBase : DebuggerBackendExtension {
    override fun id() = "Gradle LLDB"

    private fun debuggerSetupArgs(serverArgs: List<String>): String {
        return buildString {
            for (arg in serverArgs) {
                append("'$arg', ")
            }
            append("'127.0.0.1:' + debugPort, '--', task.executable")
        }
    }

    override fun initializationCode(dispatchPort: String, sertializedParams: String): List<String> {
        val params = splitParameters(sertializedParams)
        val debugServerPath = params[DEBUG_SERVER_PATH_KEY] ?: return emptyList()
        val debugServerArgs = params[DEBUG_SERVER_ARGS_KEY]?.split(":") ?: emptyList()

        return listOf(
            """
            ({
                def isInstance = { Task task, String fqn ->
                    for (def klass = task.class; klass != Object.class; klass = klass.superclass) {
                        if (klass.canonicalName == fqn) {
                            return true            
                        }
                    }        
                    return false
                }
            
                gradle.taskGraph.beforeTask { Task task ->
                    if (task.hasProperty('debugMode') 
                        && isInstance(task, 'org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest')) {
                        ForkedDebuggerHelper.setupDebugger('${id()}', task.path, '$ATTACH_BY_NAME_KEY=true', $dispatchPort)
                        task.debugMode = true
                    } else if (isInstance(task, 'org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest') 
                               || isInstance(task, 'org.gradle.api.tasks.Exec')) {
                        def debugPort = ForkedDebuggerHelper.setupDebugger('${id()}', task.path, '$ATTACH_BY_NAME_KEY=false', $dispatchPort)
                        task.args = [${debuggerSetupArgs(debugServerArgs)}] + task.args
                        task.executable = new File('$debugServerPath')
                    }
                }
            
                gradle.taskGraph.afterTask { Task task ->        
                    if (isInstance(task, 'org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest') 
                        || isInstance(task, 'org.gradle.api.tasks.Exec')) {
                        ForkedDebuggerHelper.signalizeFinish('${id()}', task.path, $dispatchPort)
                    }        
                }
            })()
            """.trimIndent()
        )
    }

    protected abstract fun getWorkspace(project: Project): WorkspaceBase

    override fun debugConfigurationSettings(
        project: Project,
        processName: String,
        processParameters: String
    ): RunnerAndConfigurationSettings {
        val params = splitParameters(processParameters)

        val settings = RunManager.getInstance(project).createConfiguration(
            processName,
            getWorkspace(project).binaryRunConfigurationType
        )

        with(settings.configuration as BinaryRunConfiguration) {
            setupFrom(findExecutable(project, processName))

            beforeRunTasks = emptyList() // incompatible with ExternalSystemTaskDebugRunner

            attachmentStrategy = AttachmentStrategy.ByName
            if (params[ATTACH_BY_NAME_KEY]?.toBoolean() == false) {
                params[ForkedDebuggerHelper.DEBUG_SERVER_PORT_KEY]?.toInt()?.let {
                    attachmentStrategy = AttachmentStrategy.ByPort(it)
                }
            }
        }

        settings.isActivateToolWindowBeforeRun = false
        return settings
    }

    private fun findExecutable(project: Project, processName: String): BinaryExecutable {
        val allAvailableExecutables = getWorkspace(project).allAvailableExecutables
        val taskName = processName.substring(processName.lastIndexOf(':') + 1)
        val projectPrefix = processName.substring(0, processName.lastIndexOf(':') + 1)

        return when {
            taskName.startsWith("run") -> {
                val executableId = taskName.removePrefix("run")
                allAvailableExecutables.find { exec ->
                    exec.projectPrefix.endsWith(projectPrefix) &&
                            exec.variants.any { variant -> variant.gradleTask.contains(executableId) }
                }
            }
            taskName.endsWith("Test") -> {
                val targetId = taskName.removeSuffix("Test")
                allAvailableExecutables.find { exec ->
                    exec.projectPrefix.endsWith(projectPrefix) &&
                            exec.targetName.contains(targetId) &&
                            exec.name.contains("test")
                }
            }
            else -> null
        } ?: throw ExecutionException("No executable for processName=$processName")
    }

    companion object {
        const val DEBUG_SERVER_PATH_KEY = "DEBUG_SERVER_PATH"
        const val DEBUG_SERVER_ARGS_KEY = "DEBUG_SERVER_ARGS"
        const val ATTACH_BY_NAME_KEY = "ATTACH_BY_NAME"
    }
}