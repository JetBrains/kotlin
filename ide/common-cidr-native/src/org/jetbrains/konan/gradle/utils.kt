package org.jetbrains.konan.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import org.jetbrains.konan.gradle.KonanProjectResolver.Companion.KONAN_MODEL_KEY
import org.jetbrains.plugins.gradle.util.GradleConstants

inline fun forEachKonanProject(
    project: Project,
    consumer: (konanModel: KonanModel, moduleNode: DataNode<ModuleData>, rootProjectPath: String) -> Unit
) {
    for (projectInfo in ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
        val projectStructure = projectInfo.externalProjectStructure ?: continue
        val projectData = projectStructure.data
        val rootProjectPath = projectData.linkedExternalProjectPath
        val modulesNodes = ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)
        for (moduleNode in modulesNodes) {
            val projectNode = ExternalSystemApiUtil.find(moduleNode, KONAN_MODEL_KEY) ?: continue
            val konanProject = projectNode.data
            consumer(konanProject, moduleNode, rootProjectPath)
        }
    }
}
