/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.RunManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.project.Project
import com.jetbrains.konan.KonanBundle
import com.jetbrains.mpp.gradle.ProjectDataServiceBase
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.readGradleProperty

class ProjectDataService : ProjectDataServiceBase() {

    override fun getWorkspace(project: Project) = ProjectWorkspace.getInstance(project)

    override fun onSuccessImport(
        imported: MutableCollection<DataNode<KotlinTargetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        super.onSuccessImport(imported, projectData, project, modelsProvider)

        val workspace = getWorkspace(project)
        workspace.locateXCProject(readGradleProperty(project, KonanBundle.message("property.xcodeproj")))
        workspace.xcProjectFile?.projectName?.let { projectName ->
            createDefaultAppleRunConfiguration(projectName, project)
        }
    }

    private fun createDefaultAppleRunConfiguration(name: String, project: Project) {
        val runManager = RunManager.getInstance(project)
        if (runManager.findConfigurationByTypeAndName(AppleRunConfigurationType.ID, name) == null) {
            runManager.addConfiguration(
                runManager.createConfiguration(name, AppleRunConfigurationType::class.java)
            )
        }
    }
}