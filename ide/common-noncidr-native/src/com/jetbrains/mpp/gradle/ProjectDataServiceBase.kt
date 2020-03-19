/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.gradle

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.konan.filterOutSystemEnvs
import com.jetbrains.konan.getKotlinNativeVersion
import com.jetbrains.mpp.*
import org.jetbrains.kotlin.gradle.KonanRunConfigurationModel
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.kotlinNativeHome

abstract class ProjectDataServiceBase : AbstractProjectDataService<KotlinTargetData, Void>() {
    override fun getTargetDataKey() = KotlinTargetData.KEY

    protected abstract fun binaryConfiguration(project: Project, executable: KonanExecutable): BinaryRunConfigurationBase

    protected abstract val configurationFactory: ConfigurationFactory

    override fun postProcess(
        toImport: Collection<DataNode<KotlinTargetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
    }

    protected fun getKonanHome(nodes: Collection<DataNode<KotlinTargetData>>): String? {
        val moduleData = nodes.firstOrNull()?.parent as? DataNode<ModuleData> ?: return null
        return moduleData.kotlinNativeHome
    }

    private fun getProjectPrefix(moduleIds: Set<String>): String {
        val id = moduleIds.firstOrNull() ?: return ""
        return id.substring(0, id.lastIndexOf(':') + 1)
    }

    protected fun collectConfigurations(
        project: Project,
        targetNodes: Collection<DataNode<KotlinTargetData>>
    ): List<BinaryRunConfigurationBase> {
        val result = ArrayList<BinaryRunConfigurationBase>()

        val executionTargets = HashMap<KonanExecutableBase, ArrayList<BinaryExecutionTarget>>()
        val runConfigurations = HashMap<KonanExecutableBase, KonanRunConfigurationModel>()

        targetNodes.forEach { node ->
            val targetName = node.data.externalName
            val projectPrefix = getProjectPrefix(node.data.moduleIds)

            node.data.konanArtifacts?.forEach { artifact ->
                val executable = KonanExecutableBase.constructFrom(
                    artifact,
                    targetName,
                    projectPrefix
                ) ?: return@forEach
                val target = BinaryExecutionTarget.constructFrom(
                    artifact,
                    executable.fullName
                ) ?: return@forEach
                executionTargets.getOrPut(executable) { ArrayList() } += target
                artifact.runConfiguration.let { runConfigurations[executable] = it }
            }
        }

        executionTargets.forEach { (executableBase, targets) ->
            val executable = KonanExecutable(executableBase, targets)
            val configuration = binaryConfiguration(project, executable).apply {
                selectedTarget = targets.firstOrNull()
                runConfigurations[executableBase]?.let {
                    workingDirectory = it.workingDirectory
                    programParameters = ParametersListUtil.join(it.programParameters)
                    envs = filterOutSystemEnvs(it.environmentVariables)
                }
            }

            result.add(configuration)
        }

        return result
    }

    protected fun updateProject(
        project: Project,
        runConfigurations: List<BinaryRunConfigurationBase>,
        workspace: WorkspaceBase,
        konanHome: String?
    ) {
        val runManager = RunManager.getInstance(project)

        workspace.executables.clear()
        konanHome?.let {
            workspace.konanHome = it
            workspace.konanVersion = getKotlinNativeVersion(it)
        }

        runConfigurations.sortedBy { it.name }.forEach { runConfiguration ->
            val executable = runConfiguration.executable ?: return@forEach
            workspace.executables.add(executable)
            val ideConfiguration = runManager.createConfiguration(executable.base.name, configurationFactory)
            (ideConfiguration.configuration as BinaryRunConfigurationBase).copyFrom(runConfiguration)
            updateConfiguration(runManager, ideConfiguration)
        }

        runManager.apply {
            selectedConfiguration = selectedConfiguration ?: allSettings.firstOrNull()
        }
    }

    // preserves manually typed run configuration data when gradle provides nothing in return
    private fun updateConfiguration(runManager: RunManager, newSettings: RunnerAndConfigurationSettings) {
        runManager.allSettings.firstOrNull { it.name == newSettings.name }?.let { oldSettings ->
            val newConfiguration = newSettings.configuration as BinaryRunConfigurationBase
            val oldConfiguration = oldSettings.configuration as? BinaryRunConfigurationBase ?: return@let

            newConfiguration.workingDirectory = newConfiguration.workingDirectory ?: oldConfiguration.workingDirectory
            newConfiguration.programParameters = newConfiguration.programParameters ?: oldConfiguration.programParameters
            if (newConfiguration.envs.isEmpty()) newConfiguration.envs = oldConfiguration.envs
            runManager.removeConfiguration(oldSettings)
        }

        runManager.addConfiguration(newSettings)
    }
}