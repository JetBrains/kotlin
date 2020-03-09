/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.CidrDebugProfile
import com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverConfiguration
import org.jdom.Element
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import javax.swing.Icon

const val DEFAULT_PASS_PARENT_ENVS = true

sealed class AttachmentStrategy
data class AttachmentByPort(val port: Int) : AttachmentStrategy()
object AttachmentByName : AttachmentStrategy()

abstract class BinaryRunConfigurationBase(
    project: Project,
    factory: ConfigurationFactory,
    var executable: KonanExecutable?
) : LocatableConfigurationBase<Any>(project, factory, executable?.base?.name),
    CommonProgramRunConfigurationParameters,
    CidrDebugProfile {

    var selectedTarget: BinaryExecutionTarget? = null
    var attachmentStrategy: AttachmentStrategy? = null // affects getState

    protected var directory: String? = null
    protected var parameters: String? = null
    protected var environmentVariables: MutableMap<String, String>? = null
    protected var passPaternalEnvs: Boolean = DEFAULT_PASS_PARENT_ENVS

    override fun getWorkingDirectory(): String? = directory
    override fun setWorkingDirectory(newDirectory: String?) {
        directory = newDirectory
    }

    override fun getEnvs(): MutableMap<String, String> {
        if (environmentVariables == null) environmentVariables = LinkedHashMap()
        return environmentVariables!!
    }

    override fun setEnvs(newEnvs: MutableMap<String, String>) {
        environmentVariables = newEnvs
    }

    override fun isPassParentEnvs(): Boolean = passPaternalEnvs

    override fun setPassParentEnvs(newPassPaternalEnvs: Boolean) {
        passPaternalEnvs = newPassPaternalEnvs
    }

    override fun setProgramParameters(newParameters: String?) {
        parameters = newParameters
    }

    override fun getProgramParameters(): String? = parameters
    override fun getIcon(): Icon? = factory?.icon

    fun copyFrom(rhs: BinaryRunConfigurationBase) {
        executable = rhs.executable
        selectedTarget = rhs.selectedTarget
        parameters = rhs.parameters
        directory = rhs.directory
        environmentVariables = rhs.environmentVariables
        passPaternalEnvs = rhs.passPaternalEnvs
    }

    private fun hostSupportsExecutable(host: KonanTarget, executable: KonanTarget?): Boolean {
        if (executable == null) return false

        return when (host) {
            KonanTarget.MACOS_X64 -> executable == KonanTarget.IOS_X64 || executable == KonanTarget.MACOS_X64
            else -> host == executable
        }
    }

    override fun canRunOn(target: ExecutionTarget): Boolean {
        if (executable == null || !hostSupportsExecutable(HostManager.host, executable?.base?.targetType)) {
            return false
        }

        return when {
            attachmentStrategy != null -> executable!!.executionTargets.any { it.isDebug }
            target is BinaryExecutionTarget -> executable!!.executionTargets.contains(target)
            else -> false
        }
    }

    protected abstract fun getWorkspace(): WorkspaceBase

    protected abstract fun lldbDriverConfiguration(env: ExecutionEnvironment): LLDBDriverConfiguration

    private fun commandLineState(env: ExecutionEnvironment, lldbConfiguration: LLDBDriverConfiguration?): RunProfileState? {
        val execFile = selectedTarget?.productFile ?: return null
        return KonanCommandLineState(env, this, execFile, lldbConfiguration)
    }

    private fun externalSystemState(env: ExecutionEnvironment, lldbConfiguration: LLDBDriverConfiguration): RunProfileState {
        val dummyFactory = object : ConfigurationFactory(type) {
            override fun createTemplateConfiguration(project: Project): RunConfiguration {
                return GradleRunConfiguration(project, this, "")
            }
        }
        val dummyConfiguration = GradleRunConfiguration(project, dummyFactory, "")
        dummyConfiguration.settings.externalProjectPath = ""
        return KonanExternalSystemState(this, project, env, dummyConfiguration, lldbConfiguration)
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        if (DefaultDebugExecutor.EXECUTOR_ID != executor.id) {
            return commandLineState(env, null)
        }

        val lldbConfiguration = lldbDriverConfiguration(env)

        return if (attachmentStrategy != null)
            externalSystemState(env, lldbConfiguration)
        else
            commandLineState(env, lldbConfiguration)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val base = KonanExecutableBase.readFromXml(element) ?: return
        executable = getWorkspace().executables.firstOrNull { it.base == base }
        selectedTarget = executable?.executionTargets?.firstOrNull()

        parameters = element.getAttributeValue(XmlRunConfiguration.attributeParameters)
        directory = element.getAttributeValue(XmlRunConfiguration.attributeDirectory)
        passPaternalEnvs = element.getAttributeValue(XmlRunConfiguration.attributePassParent)?.toBoolean() ?: DEFAULT_PASS_PARENT_ENVS
        environmentVariables = LinkedHashMap()
        EnvironmentVariablesComponent.readExternal(element, environmentVariables)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        executable?.base?.writeToXml(element)

        parameters?.let {
            if (it.isNotEmpty()) element.setAttribute(XmlRunConfiguration.attributeParameters, it)
        }
        directory?.let { element.setAttribute(XmlRunConfiguration.attributeDirectory, it) }
        if (passPaternalEnvs != DEFAULT_PASS_PARENT_ENVS) {
            element.setAttribute(XmlRunConfiguration.attributePassParent, passPaternalEnvs.toString())
        }

        environmentVariables?.let { EnvironmentVariablesComponent.writeExternal(element, it) }
    }
}