package com.jetbrains.mobile.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.mobile.execution.AndroidEmulator
import com.jetbrains.mobile.execution.MobileDeviceService
import com.jetbrains.mobile.execution.MobileRunConfigurationBase
import com.jetbrains.mobile.execution.createDefaults
import com.jetbrains.mobile.isCommonMain

/** Creates default run configurations & contributes class loader to external system deserialization */
class AndroidProjectDataService : AbstractProjectDataService<AndroidProjectModel, Module>() {
    override fun getTargetDataKey(): Key<AndroidProjectModel> =
        AndroidProjectResolver.KEY

    override fun onSuccessImport(
        imported: Collection<DataNode<AndroidProjectModel>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        val modules = modelsProvider.getModules(projectData ?: return)
            .filter { it.isCommonMain }
        val device = MobileDeviceService.getInstance(project).getAndroidDevices()
            .filterIsInstance<AndroidEmulator>()
            .firstOrNull() ?: return
        MobileRunConfigurationBase.createDefaults(project, modules, device)
    }
}