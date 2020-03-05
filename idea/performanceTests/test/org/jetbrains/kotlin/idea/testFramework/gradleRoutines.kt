/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.plugins.gradle.service.project.open.setupGradleSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleLog
import java.io.File
import kotlin.test.assertNotNull

fun refreshGradleProject(projectPath: String, project: Project) {
    _importProject(File(projectPath).absolutePath, project)

    dispatchAllInvocationEvents()
}

/**
 * inspired by org.jetbrains.plugins.gradle.service.project.open.importProject(projectDirectory, project)
 */
private fun _importProject(projectPath: String, project: Project) {
    GradleLog.LOG.info("Import project at $projectPath")
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    assertNotNull(projectSdk, "project SDK not found for ${project.name} at $projectPath")
    val gradleProjectSettings = GradleProjectSettings()
    setupGradleSettings(gradleProjectSettings, projectPath, project, projectSdk)

    _attachGradleProjectAndRefresh(gradleProjectSettings, project)
}

/**
 * inspired by org.jetbrains.plugins.gradle.service.project.open.attachGradleProjectAndRefresh(gradleProjectSettings, project)
 * except everything is MODAL_SYNC
 */
private fun _attachGradleProjectAndRefresh(
    gradleProjectSettings: GradleProjectSettings,
    project: Project
) {
    val externalProjectPath = gradleProjectSettings.externalProjectPath
    ExternalProjectsManagerImpl.getInstance(project).runWhenInitialized {
        DumbService.getInstance(project).runWhenSmart {
            ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID)
        }
    }
    //ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(false)

    ExternalProjectsManagerImpl.disableProjectWatcherAutoUpdate(project)
    val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    if (settings.getLinkedProjectSettings(externalProjectPath) == null) {
        settings.linkProject(gradleProjectSettings)
    }
    //ExternalSystemUtil.refreshProject(project, GradleConstants.SYSTEM_ID, externalProjectPath, false, ProgressExecutionMode.MODAL_SYNC)

    val progressExecutionMode = ProgressExecutionMode.MODAL_SYNC
    val externalSystemId = GradleConstants.SYSTEM_ID
    val callback = object : ExternalProjectRefreshCallback {
        override fun onFailure(errorMessage: String, errorDetails: String?) {
            super.onFailure(errorMessage, errorDetails)
            throw RuntimeException(errorMessage)
        }

        override fun onFailure(
            externalTaskId: ExternalSystemTaskId,
            errorMessage: String,
            errorDetails: String?
        ) {
            super.onFailure(externalTaskId, errorMessage, errorDetails)
            throw RuntimeException(errorMessage)
        }

        override fun onSuccess(externalProject: DataNode<ProjectData>?) {
            if (externalProject == null) {
                return
            }
            val synchronous = progressExecutionMode == ProgressExecutionMode.MODAL_SYNC
            ServiceManager.getService(
                ProjectDataManager::class.java
            ).importData(externalProject, project, synchronous)
        }
    }
    ExternalSystemUtil.refreshProject(project, externalSystemId, externalProjectPath, callback, false, progressExecutionMode, true)
}
