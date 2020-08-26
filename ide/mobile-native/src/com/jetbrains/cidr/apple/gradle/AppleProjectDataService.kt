package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.apple.gradle.AppleProjectResolver.Companion.APPLE_PROJECT
import com.jetbrains.mobile.execution.AppleSimulator
import com.jetbrains.mobile.execution.MobileDeviceService
import com.jetbrains.mobile.execution.MobileRunConfigurationBase
import com.jetbrains.mobile.execution.createDefaults
import com.jetbrains.mobile.gradle.forEachModule
import com.jetbrains.mobile.isCommonMain

class AppleProjectDataService : AbstractProjectDataService<AppleProjectModel, Module>() {
    override fun getTargetDataKey(): Key<AppleProjectModel> = APPLE_PROJECT

    override fun postProcess(
        toImport: Collection<DataNode<AppleProjectModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        GradleAppleWorkspace.getInstance(project).update()
    }

    override fun onSuccessImport(
        imported: Collection<DataNode<AppleProjectModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        val modules = modelsProvider.getModules(projectData ?: return)
            .filter { it.isCommonMain }
        val device = MobileDeviceService.getInstance(project).getAppleDevices()
            .filterIsInstance<AppleSimulator>()
            .firstOrNull() ?: return
        MobileRunConfigurationBase.createDefaults(project, modules, device)
    }

    companion object {
        inline fun forEachProject(project: Project, consumer: (AppleProjectModel, ModuleData, String) -> Unit) {
            project.forEachModule(APPLE_PROJECT, consumer)
        }

        fun hasAnyProject(project: Project): Boolean {
            forEachProject(project) { _, _, _ ->
                return true
            }
            return false
        }
    }
}
