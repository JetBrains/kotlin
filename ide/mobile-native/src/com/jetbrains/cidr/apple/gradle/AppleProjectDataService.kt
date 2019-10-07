package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.apple.gradle.AppleProjectResolver.Companion.APPLE_PROJECT
import org.jetbrains.plugins.gradle.util.GradleConstants

class AppleProjectDataService : AbstractProjectDataService<AppleProjectModel, Module>() {
    override fun getTargetDataKey(): Key<AppleProjectModel> = APPLE_PROJECT

    override fun postProcess(
        toImport: MutableCollection<DataNode<AppleProjectModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        GradleAppleWorkspace.getInstance(project).update()
    }

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<AppleProjectModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        // TODO Need to create run configurations?
    }

    companion object {
        inline fun forEachProject(project: Project, consumer: (AppleProjectModel, ModuleData, String) -> Unit) {
            for (projectInfo in ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
                val projectStructure = projectInfo.externalProjectStructure ?: continue
                val rootProjectPath = projectStructure.data.linkedExternalProjectPath
                for (moduleNode in ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)) {
                    consumer(
                        ExternalSystemApiUtil.find(moduleNode, APPLE_PROJECT)?.data ?: continue,
                        moduleNode.data,
                        rootProjectPath
                    )
                }
            }
        }

        fun hasAnyProject(project: Project): Boolean {
            forEachProject(project) { _, _, _ ->
                return true
            }
            return false
        }
    }
}
