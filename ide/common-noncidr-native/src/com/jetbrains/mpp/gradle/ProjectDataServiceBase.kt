/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.gradle

import com.intellij.execution.RunManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.konan.filterOutSystemEnvs
import com.jetbrains.konan.getKotlinNativeVersion
import com.jetbrains.mpp.BinaryExecutionTarget
import com.jetbrains.mpp.KonanExecutable
import com.jetbrains.mpp.KonanExecutableBase
import com.jetbrains.mpp.WorkspaceBase
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import org.jetbrains.kotlin.gradle.KonanRunConfigurationModel
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.kotlinNativeHome

abstract class ProjectDataServiceBase : AbstractProjectDataService<KotlinTargetData, Void>() {
    override fun getTargetDataKey() = KotlinTargetData.KEY
    protected abstract fun getWorkspace(project: Project): WorkspaceBase

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<KotlinTargetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        super.onSuccessImport(imported, projectData, project, modelsProvider)

        val importedRunConfigurationParameters = collectRunConfigurationParameters(imported)
        updateWorkspace(
            getWorkspace(project),
            getKonanHome(imported),
            importedRunConfigurationParameters.map { it.executable }
        )
        updateRunConfigurations(
            project,
            importedRunConfigurationParameters
        )

        //default selection
        RunManager.getInstance(project).apply {
            selectedConfiguration = selectedConfiguration ?: allSettings.firstOrNull()
        }
    }

    private fun collectRunConfigurationParameters(
        targetNodes: Collection<DataNode<KotlinTargetData>>
    ): List<RunConfigurationParameters> {
        val result = ArrayList<RunConfigurationParameters>()

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
            val rc = runConfigurations[executableBase]
            result.add(RunConfigurationParameters(
                KonanExecutable(executableBase, targets),
                targets.firstOrNull(),
                rc?.workingDirectory,
                rc?.let { ParametersListUtil.join(rc.programParameters) },
                rc?.let { filterOutSystemEnvs(rc.environmentVariables) } ?: emptyMap()
            ))
        }

        return result
    }

    private fun getProjectPrefix(moduleIds: Set<String>): String =
        moduleIds.firstOrNull()?.let { id ->
            id.substring(0, id.lastIndexOf(':') + 1)
        } ?: ""

    private fun getKonanHome(nodes: Collection<DataNode<KotlinTargetData>>): String? {
        val moduleData = nodes.firstOrNull()?.parent as? DataNode<ModuleData> ?: return null
        return moduleData.kotlinNativeHome
    }

    private fun updateWorkspace(
        workspace: WorkspaceBase,
        konanHome: String?,
        executables: List<KonanExecutable>
    ) {
        konanHome?.let {
            workspace.konanHome = it
            workspace.konanVersion = getKotlinNativeVersion(it)
        }

        workspace.executables.apply {
            clear()
            addAll(executables)
        }
    }

    private fun updateRunConfigurations(
        project: Project,
        runConfigurationParameters: List<RunConfigurationParameters>
    ) {
        val type = getWorkspace(project).binaryRunConfigurationType
        val runManager = RunManager.getInstance(project)

        runConfigurationParameters
            .sortedBy { it.executable.base.name }
            .forEach { importedConfig ->
                val new = runManager.createConfiguration(importedConfig.executable.base.name, type)
                val old = runManager.findConfigurationByTypeAndName(new.type, new.name)

                (new.configuration as BinaryRunConfiguration).setup(importedConfig, old?.configuration as? BinaryRunConfiguration)

                runManager.removeConfiguration(old)
                runManager.addConfiguration(new)
            }
    }

    private fun BinaryRunConfiguration.setup(importedConfig: RunConfigurationParameters, old: BinaryRunConfiguration?) {
        if (old != null) {
            executable = old.executable
            selectedTarget = old.selectedTarget
            workingDirectory = old.workingDirectory
            programParameters = old.programParameters
            envs = old.envs
        } else {
            executable = importedConfig.executable
            selectedTarget = importedConfig.target
            workingDirectory = importedConfig.workingDirectory
            programParameters = importedConfig.programParameters
            envs = importedConfig.envs
        }
    }

    private class RunConfigurationParameters(
        val executable: KonanExecutable,
        val target: BinaryExecutionTarget?,
        val workingDirectory: String?,
        val programParameters: String?,
        val envs: Map<String, String>
    )
}