/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.application.ApplicationManager
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
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.konan.settings.KonanArtifact
import org.jetbrains.konan.settings.KonanModelProvider
import org.jetbrains.konan.settings.isExecutable
import org.jetbrains.kotlin.gradle.plugin.model.KonanModel
import org.jetbrains.plugins.gradle.util.GradleConstants

class KonanProjectDataService : AbstractProjectDataService<KonanModel, Module>() {

    override fun getTargetDataKey(): Key<KonanModel> = KonanProjectResolver.KONAN_MODEL_KEY

    override fun postProcess(
        toImport: Collection<DataNode<KonanModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
    }

    override fun onSuccessImport(
        imported: Collection<DataNode<KonanModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        if (projectData?.owner != GradleConstants.SYSTEM_ID) return
        project.messageBus.syncPublisher(KonanModelProvider.RELOAD_TOPIC).run()
    }

    companion object {
        @JvmStatic
        fun forEachKonanProject(
            project: Project,
            consumer: (konanProject: KonanModel, moduleData: ModuleData, rootProjectPath: String) -> Unit
        ) {
            for (projectInfo in ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID)) {
                val projectStructure = projectInfo.externalProjectStructure ?: continue
                val projectData = projectStructure.data
                val rootProjectPath = projectData.linkedExternalProjectPath
                val modulesNodes = ExternalSystemApiUtil.findAll(projectStructure, ProjectKeys.MODULE)
                for (moduleNode in modulesNodes) {
                    val projectNode = ExternalSystemApiUtil.find<KonanModel>(moduleNode, KonanProjectResolver.KONAN_MODEL_KEY)
                    if (projectNode != null) {
                        val konanProject = projectNode.data
                        val moduleData = moduleNode.data
                        consumer(konanProject, moduleData, rootProjectPath)
                    }
                }
            }
        }
    }

}
