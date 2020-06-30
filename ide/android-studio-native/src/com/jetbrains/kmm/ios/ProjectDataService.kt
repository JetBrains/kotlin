/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.project.Project
import com.jetbrains.konan.KonanBundle
import com.jetbrains.kmm.ios.execution.BinaryRunConfiguration
import com.jetbrains.kmm.ios.execution.BinaryRunConfigurationType
import com.jetbrains.mpp.KonanExecutable
import com.jetbrains.mpp.gradle.ProjectDataServiceBase
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.readGradleProperty
import org.jetbrains.plugins.gradle.util.GradleConstants

class ProjectDataService : ProjectDataServiceBase() {
    override val configurationFactory = BinaryRunConfigurationType.instance.factory

    override fun getTargetDataKey() = KotlinTargetData.KEY

    override fun binaryConfiguration(project: Project, executable: KonanExecutable) =
        BinaryRunConfiguration(project, configurationFactory, executable)

    override fun onSuccessImport(
        imported: Collection<DataNode<KotlinTargetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        val workspace = ProjectWorkspace.getInstance(project)
        readGradleProperty(project, KonanBundle.message("property.xcodeproj"))?.let {
            workspace.locateXCProject(it)
        }

        if (projectData?.owner != GradleConstants.SYSTEM_ID) return

        val configurations = collectConfigurations(project, imported)
        updateProject(project, configurations, workspace, getKonanHome(imported))
    }
}