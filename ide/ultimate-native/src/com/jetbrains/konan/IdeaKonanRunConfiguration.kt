/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.konan

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.ExecutionTarget
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.execution.debugger.CidrDebugProfile
import org.jdom.Element
import org.jetbrains.kotlin.idea.run.LocatableConfigurationBaseAny
import org.jetbrains.kotlin.konan.target.HostManager
import javax.swing.Icon

const val DEFAULT_PASS_PARENT_ENVS = true

class IdeaKonanRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    var executable: KonanExecutable?
) : LocatableConfigurationBaseAny(project, factory, executable?.base?.name), CommonProgramRunConfigurationParameters, CidrDebugProfile {

    var selectedTarget: IdeaKonanExecutionTarget? = null

    private var parameters: String? = null
    private var directory: String? = null
    private var envs: MutableMap<String, String>? = null
    private var passPaternalEnvs: Boolean = DEFAULT_PASS_PARENT_ENVS

    fun copyFrom(rhs: IdeaKonanRunConfiguration) {
        executable = rhs.executable
        selectedTarget = rhs.selectedTarget
        parameters = rhs.parameters
        directory = rhs.directory
        envs = rhs.envs
        passPaternalEnvs = rhs.passPaternalEnvs
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return IdeaKonanRunConfigurationSettingsEditor(project)
    }

    override fun getWorkingDirectory(): String? = directory

    override fun setWorkingDirectory(newDirectory: String?) {
        directory = newDirectory
    }

    override fun getEnvs(): MutableMap<String, String> {
        if (envs == null) envs = LinkedHashMap()
        return envs!!
    }

    override fun setEnvs(newEnvs: MutableMap<String, String>) {
        envs = newEnvs
    }

    override fun isPassParentEnvs(): Boolean = passPaternalEnvs

    override fun setPassParentEnvs(newPassPaternalEnvs: Boolean) {
        passPaternalEnvs = newPassPaternalEnvs
    }

    override fun setProgramParameters(newParameters: String?) {
        parameters = newParameters
    }

    override fun getProgramParameters(): String? = parameters

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        val runFile = selectedTarget?.productFile ?: return null
        return KonanCommandLineState(env, IdeaKonanLauncher(this, runFile))
    }

    override fun canRunOn(target: ExecutionTarget): Boolean {
        if (HostManager.host != executable?.base?.targetType) return false
        return target is IdeaKonanExecutionTarget && (executable?.executionTargets?.contains(target) ?: false)
    }

    override fun getIcon(): Icon? = factory?.icon

    override fun readExternal(element: Element) {
        super.readExternal(element)
        val base = KonanExecutableBase.readFromXml(element) ?: return
        executable = IdeaKonanWorkspace.getInstance(project).executables.firstOrNull { it.base == base }
        selectedTarget = executable?.executionTargets?.firstOrNull()

        parameters = element.getAttributeValue(XmlRunConfiguration.attributeParameters)
        directory = element.getAttributeValue(XmlRunConfiguration.attributeDirectory)
        passPaternalEnvs = element.getAttributeValue(XmlRunConfiguration.attributePassParent)?.toBoolean() ?: DEFAULT_PASS_PARENT_ENVS
        envs = LinkedHashMap()
        EnvironmentVariablesComponent.readExternal(element, envs)
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

        envs?.let { EnvironmentVariablesComponent.writeExternal(element, it) }
    }
}