/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mpp.gradle

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.project.Project
import com.jetbrains.mpp.*
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.plugins.gradle.util.GradleConstants

class MPPProjectDataService : ProjectDataServiceBase() {

    override fun getTargetDataKey() = KotlinTargetData.KEY

    override val configurationFactory = MPPBinaryRunConfigurationType.instance.factory

    override fun binaryConfiguration(project: Project, executable: KonanExecutable) =
        MPPBinaryRunConfiguration(project, configurationFactory, executable)

    override fun onSuccessImport(
        imported: Collection<DataNode<KotlinTargetData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModelsProvider
    ) {
        val workspace = MPPWorkspace.getInstance(project)

        if (projectData?.owner != GradleConstants.SYSTEM_ID) return

        val configurations = collectConfigurations(project, imported)
        updateProject(project, configurations, workspace, getKonanHome(imported))
    }
}