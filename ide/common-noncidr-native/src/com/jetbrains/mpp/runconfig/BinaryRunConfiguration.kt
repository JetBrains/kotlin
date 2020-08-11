/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.runconfig

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.CidrDebugProfile
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.KonanCommandLineState
import com.jetbrains.mpp.debugger.KonanExternalSystemState
import com.jetbrains.mpp.workspace.State.readFromXml
import com.jetbrains.mpp.workspace.State.writeToXml
import com.jetbrains.mpp.workspace.WorkspaceBase
import org.jdom.Element
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import java.io.File

class BinaryRunConfiguration(
    private val workspace: WorkspaceBase,
    project: Project,
    factory: ConfigurationFactory
) : LocatableConfigurationBase<Any>(project, factory),
    CommonProgramRunConfigurationParameters,
    CidrDebugProfile {

    var executable: BinaryExecutable? = workspace.allAvailableExecutables.firstOrNull()
    var variant: BinaryExecutable.Variant? = executable?.variants?.firstOrNull()
    var attachmentStrategy: AttachmentStrategy? = null // affects getState

    private var directory: String? = null
    override fun getWorkingDirectory(): String? = directory
    override fun setWorkingDirectory(newDirectory: String?) {
        directory = newDirectory
    }

    private var environmentVariables: Map<String, String> = LinkedHashMap()
    override fun getEnvs(): Map<String, String> = environmentVariables
    override fun setEnvs(newEnvs: Map<String, String>) {
        environmentVariables = newEnvs
    }

    private var passPaternalEnvs: Boolean = true
    override fun isPassParentEnvs(): Boolean = passPaternalEnvs
    override fun setPassParentEnvs(newPassPaternalEnvs: Boolean) {
        passPaternalEnvs = newPassPaternalEnvs
    }

    private var parameters: String? = null
    override fun getProgramParameters(): String? = parameters
    override fun setProgramParameters(newParameters: String?) {
        parameters = newParameters
    }

    fun setupFrom(exec: BinaryExecutable) {
        executable = exec
        variant = exec.variants.firstOrNull { it is BinaryExecutable.Variant.Debug } ?: exec.variants.firstOrNull()
        workingDirectory = variant?.params?.workingDirectory
        programParameters = variant?.params?.programParameters ?: ""
        envs = variant?.params?.environmentVariables ?: emptyMap()
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        BinaryRunConfigurationSettingsEditor(workspace.allAvailableExecutables)

    override fun checkConfiguration() {
        val selectedExecutable = executable
        val selectedVariant = variant
        when {
            selectedExecutable == null -> {
                throw RuntimeConfigurationError("There is no executable.")
            }
            !workspace.allAvailableExecutables.contains(selectedExecutable) -> {
                throw RuntimeConfigurationError("${selectedExecutable.name} was not found in project.")
            }
            selectedVariant == null -> {
                throw RuntimeConfigurationError("There is no variant for launch.")
            }
            !selectedExecutable.variants.contains(selectedVariant) -> {
                throw RuntimeConfigurationError("Selected variant ${selectedVariant.name} is incompatible with executable.")
            }
        }
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        if (executor.id != DefaultDebugExecutor.EXECUTOR_ID) return commandLineState(env, null)

        return if (attachmentStrategy != null)
            externalSystemState(env, workspace.lldbDriverConfiguration)
        else
            commandLineState(env, workspace.lldbDriverConfiguration)
    }

    private fun externalSystemState(env: ExecutionEnvironment, lldbConfiguration: LLDBDriverConfiguration): RunProfileState {
        val dummyFactory = object : ConfigurationFactory(type) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration =
                GradleRunConfiguration(project, this, "")
        }
        val dummyConfiguration = GradleRunConfiguration(project, dummyFactory, "").apply {
            settings.externalProjectPath = ""
        }
        return KonanExternalSystemState(
            this,
            project,
            lldbConfiguration,
            env,
            dummyConfiguration
        )
    }

    private fun commandLineState(env: ExecutionEnvironment, lldbConfiguration: LLDBDriverConfiguration?): RunProfileState? {
        val execFile = variant?.file ?: return null
        return KonanCommandLineState(
            env,
            this,
            execFile,
            lldbConfiguration
        )
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        readFromXml(element, File(project.basePath!!))
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        writeToXml(element, File(project.basePath!!))
    }
}