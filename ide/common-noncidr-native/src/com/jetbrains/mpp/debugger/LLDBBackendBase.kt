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
import com.jetbrains.mpp.KonanExecutable
import com.jetbrains.mpp.WorkspaceBase
import com.jetbrains.mpp.runconfig.AttachmentStrategy
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration

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
        val runManager = RunManager.getInstance(project)
        val isTest = processName.endsWith("Test")
        val executableForProcess = findExecutable(project, processName)
        val runConfigForProcess = runManager.findRunConfiguration(executableForProcess)
        val params = splitParameters(processParameters)

        val settings = runManager.createConfiguration(processName, getWorkspace(project).binaryRunConfigurationType)
        with(settings.configuration as BinaryRunConfiguration) {
            //setup
            executable = runConfigForProcess.executable
            selectedTarget = runConfigForProcess.selectedTarget
            workingDirectory = runConfigForProcess.workingDirectory
            programParameters = runConfigForProcess.programParameters
            envs = runConfigForProcess.envs

            if (isTest) {
                programParameters = "$processParameters --ktest_no_exit_code"
            }

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

    private fun findExecutable(project: Project, processName: String): KonanExecutable {
        val workspace = getWorkspace(project)
        val taskName = processName.substring(processName.lastIndexOf(':') + 1)
        val projectPrefix = processName.substring(0, processName.lastIndexOf(':') + 1)

        val result = when {
            taskName.startsWith("run") -> {
                val executableId = taskName.removePrefix("run")
                workspace.executables.find {
                    it.base.projectPrefix == projectPrefix &&
                            it.executionTargets.any { t -> t.gradleTask.contains(executableId) }
                }
            }
            taskName.endsWith("Test") -> {
                val targetId = taskName.removeSuffix("Test")
                workspace.executables.find {
                    it.base.projectPrefix.endsWith(projectPrefix) &&
                            it.base.name.contains(targetId) && it.base.name.contains("test")
                }
            }
            else -> null
        }

        return result ?: throw ExecutionException("No executable for processName=$processName")
    }

    private fun RunManager.findRunConfiguration(konanExecutable: KonanExecutable): BinaryRunConfiguration {
        val result = allSettings.firstOrNull {
            (it.configuration as? BinaryRunConfiguration)?.executable == konanExecutable
        } ?: throw ExecutionException("No configuration for executable=${konanExecutable.base}")
        return result.configuration as BinaryRunConfiguration
    }

    companion object {
        const val DEBUG_SERVER_PATH_KEY = "DEBUG_SERVER_PATH"
        const val DEBUG_SERVER_ARGS_KEY = "DEBUG_SERVER_ARGS"
        const val ATTACH_BY_NAME_KEY = "ATTACH_BY_NAME"
    }
}