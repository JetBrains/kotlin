/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.workspace

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
import com.jetbrains.mpp.BinaryExecutable
import com.jetbrains.mpp.RunParameters
import com.jetbrains.mpp.runconfig.BinaryRunConfiguration
import org.jetbrains.kotlin.gradle.KonanArtifactModel
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.kotlinNativeHome
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

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

        val importedBinaryExecutable = collectBinaryExecutables(imported)
        updateWorkspace(
            getWorkspace(project),
            getKonanHome(imported),
            importedBinaryExecutable
        )
        updateRunConfigurations(project)

        //default selection
        RunManager.getInstance(project).apply {
            selectedConfiguration = selectedConfiguration ?: allSettings.firstOrNull()
        }
    }

    private fun collectBinaryExecutables(
        targetNodes: Collection<DataNode<KotlinTargetData>>
    ): List<BinaryExecutable> {

        data class ImportedExecutable(
            val target: KonanTarget,
            val targetName: String,
            val execName: String,
            val projectName: String
        )

        data class ImportedVariant(
            val gradleTask: String,
            val file: File,
            val params: RunParameters
        )

        val imported = HashMap<ImportedExecutable, ArrayList<ImportedVariant>>()

        targetNodes.forEach { node ->
            val projectPrefix = getProjectPrefix(node.data.moduleIds)

            node.data.konanArtifacts?.forEach art@{ artifact ->
                val konanTarget = artifact.getSupportedTargetOrNull() ?: return@art
                val exec = ImportedExecutable(
                    konanTarget,
                    artifact.targetName,
                    artifact.executableName,
                    projectPrefix
                )
                val params = ImportedVariant(
                    artifact.buildTaskPath,
                    artifact.file,
                    RunParameters(
                        artifact.runConfiguration.workingDirectory,
                        ParametersListUtil.join(artifact.runConfiguration.programParameters),
                        filterOutSystemEnvs(artifact.runConfiguration.environmentVariables)
                    )
                )
                imported.getOrPut(exec) { ArrayList() } += params
            }
        }

        return imported.map { (exec, variants) ->
            BinaryExecutable(
                exec.target,
                exec.targetName,
                exec.execName,
                exec.projectName,
                variants.map { importedVariant ->
                    if (importedVariant.gradleTask.contains("debug", true)) {
                        BinaryExecutable.Variant.Debug(
                            importedVariant.gradleTask,
                            importedVariant.file,
                            importedVariant.params
                        )
                    } else {
                        BinaryExecutable.Variant.Release(
                            importedVariant.gradleTask,
                            importedVariant.file,
                            importedVariant.params
                        )
                    }
                }
            )
        }
    }

    private fun KonanArtifactModel.getSupportedTargetOrNull(): KonanTarget? {
        if (isTests) return null
        if (type != CompilerOutputKind.PROGRAM.name) return null
        return KonanTarget.predefinedTargets[targetPlatform]
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
        executables: List<BinaryExecutable>
    ) {
        konanHome?.let {
            workspace.konanHome = it
            workspace.konanVersion = getKotlinNativeVersion(it)
        }

        workspace.allAvailableExecutables.apply {
            clear()
            addAll(executables)
        }
    }

    private fun updateRunConfigurations(project: Project) {
        val workspace = getWorkspace(project)
        val runManager = RunManager.getInstance(project)
        val type = workspace.binaryRunConfigurationType

        workspace.allAvailableExecutables
            .sortedBy { it.targetName }
            .forEach { exec ->
                val new = runManager.createConfiguration(exec.name, type)
                val old = runManager.findConfigurationByTypeAndName(new.type, new.name)

                (new.configuration as BinaryRunConfiguration).setup(exec, old?.configuration as? BinaryRunConfiguration)

                runManager.removeConfiguration(old)
                runManager.addConfiguration(new)
            }
    }

    private fun BinaryRunConfiguration.setup(imported: BinaryExecutable, old: BinaryRunConfiguration?) {
        if (old != null) {
            executable = old.executable
            variant = old.variant
            workingDirectory = old.workingDirectory
            programParameters = old.programParameters
            envs = old.envs
        } else {
            val variant = imported.variants.firstOrNull { it is BinaryExecutable.Variant.Debug } ?: imported.variants.firstOrNull()
            executable = imported
            this.variant = variant
            workingDirectory = variant?.params?.workingDirectory
            programParameters = variant?.params?.programParameters ?: ""
            envs = variant?.params?.environmentVariables ?: emptyMap()
        }
    }
}