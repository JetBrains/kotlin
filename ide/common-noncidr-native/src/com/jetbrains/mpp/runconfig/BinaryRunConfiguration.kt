/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.runconfig

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.DefaultExecutionTarget
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.CidrDebugProfile
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import com.jetbrains.konan.WorkspaceXML
import com.jetbrains.mpp.*
import com.jetbrains.mpp.debugger.KonanExternalSystemState
import org.jdom.Element
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import javax.swing.Icon

class BinaryRunConfiguration(
    private val workspace: WorkspaceBase,
    project: Project,
    factory: ConfigurationFactory,
    konanExecutable: KonanExecutable?
) : LocatableConfigurationBase<Any>(project, factory, konanExecutable?.base?.name),
    CommonProgramRunConfigurationParameters,
    CidrDebugProfile {

    var executable: KonanExecutable? = konanExecutable
    var selectedTarget: BinaryExecutionTarget? = null
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

    override fun getIcon(): Icon? = factory?.icon

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        BinaryRunConfigurationSettingsEditor(workspace.executables)

    override fun canRunOn(target: ExecutionTarget): Boolean {
        val exec = executable ?: return false
        if (!hostSupportsExecutable(HostManager.host, exec.base.targetType)) return false

        return when (target) {
            is DefaultExecutionTarget -> // being called during Gradle task debug run
                attachmentStrategy != null && exec.executionTargets.any { it.isDebug }
            is BinaryExecutionTarget -> // straightforward execution
                exec.executionTargets.contains(target)
            else -> false
        }
    }

    private fun hostSupportsExecutable(host: KonanTarget, executable: KonanTarget) = when (host) {
        KonanTarget.MACOS_X64 -> executable == KonanTarget.IOS_X64 || executable == KonanTarget.MACOS_X64
        else -> host == executable
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
            env,
            dummyConfiguration,
            lldbConfiguration
        )
    }

    private fun commandLineState(env: ExecutionEnvironment, lldbConfiguration: LLDBDriverConfiguration?): RunProfileState? {
        val execFile = selectedTarget?.productFile ?: return null
        return KonanCommandLineState(
            env,
            this,
            execFile,
            lldbConfiguration
        )
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        val base = KonanExecutableBase.readFromXml(element) ?: return
        executable = workspace.executables.firstOrNull { it.base == base }
        selectedTarget = executable?.executionTargets?.firstOrNull()

        parameters = element.getAttributeValue(WorkspaceXML.RunConfiguration.attributeParameters)
        directory = element.getAttributeValue(WorkspaceXML.RunConfiguration.attributeDirectory)
        element.getAttributeValue(WorkspaceXML.RunConfiguration.attributePassParent)?.let { passPaternalEnvs = it.toBoolean() }
        environmentVariables = LinkedHashMap()
        EnvironmentVariablesComponent.readExternal(element, environmentVariables)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)

        executable?.base?.writeToXml(element)
        parameters?.let { element.setAttribute(WorkspaceXML.RunConfiguration.attributeParameters, it) }
        directory?.let { element.setAttribute(WorkspaceXML.RunConfiguration.attributeDirectory, it) }
        element.setAttribute(WorkspaceXML.RunConfiguration.attributePassParent, passPaternalEnvs.toString())
        EnvironmentVariablesComponent.writeExternal(element, environmentVariables)
    }
}