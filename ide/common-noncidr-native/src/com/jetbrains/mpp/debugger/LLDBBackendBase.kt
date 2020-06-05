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
import com.jetbrains.mpp.AttachmentByName
import com.jetbrains.mpp.AttachmentByPort
import com.jetbrains.mpp.BinaryRunConfigurationBase
import com.jetbrains.mpp.KonanExecutable

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

    protected abstract fun binaryConfiguration(runManager: RunManager, konanExecutable: KonanExecutable): BinaryRunConfigurationBase

    protected abstract fun runSettings(runManager: RunManager, processName: String): RunnerAndConfigurationSettings

    protected abstract fun findExecutable(project: Project, processName: String): KonanExecutable?

    override fun debugConfigurationSettings(
        project: Project,
        processName: String,
        processParameters: String
    ): RunnerAndConfigurationSettings {
        val runManager = RunManager.getInstance(project)
        val isTest = processName.endsWith("Test")
        val executable = findExecutable(project, processName)
            ?: throw ExecutionException("No executable for processName=$processName")

        val params = splitParameters(processParameters)

        val settings = runSettings(runManager, processName)
        with(settings.configuration as BinaryRunConfigurationBase) {
            if (isTest) {
                programParameters = "$processParameters --ktest_no_exit_code"
            }
            copyFrom(binaryConfiguration(runManager, executable))
            attachmentStrategy = AttachmentByName
            if (params[ATTACH_BY_NAME_KEY]?.toBoolean() == false) {
                params[ForkedDebuggerHelper.DEBUG_SERVER_PORT_KEY]?.toInt()?.let {
                    attachmentStrategy = AttachmentByPort(it)
                }
            }
        }

        settings.isActivateToolWindowBeforeRun = false
        return settings
    }

    companion object {
        const val DEBUG_SERVER_PATH_KEY = "DEBUG_SERVER_PATH"
        const val DEBUG_SERVER_ARGS_KEY = "DEBUG_SERVER_ARGS"
        const val ATTACH_BY_NAME_KEY = "ATTACH_BY_NAME"
    }
}