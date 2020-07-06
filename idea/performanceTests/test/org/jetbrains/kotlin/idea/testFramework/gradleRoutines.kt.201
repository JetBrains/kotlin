/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.jetbrains.plugins.gradle.service.project.open.setupGradleSettings
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleLog
import java.io.File
import kotlin.test.assertNotNull

fun refreshGradleProject(projectPath: String, project: Project) {
    _importProject(File(projectPath).absolutePath, project)

    dispatchAllInvocationEvents()
}

const val GRADLE_JDK_NAME = "Gradle JDK"

/**
 * inspired by org.jetbrains.plugins.gradle.service.project.open.importProject(projectDirectory, project)
 */
private fun _importProject(projectPath: String, project: Project) {
    GradleLog.LOG.info("Import project at $projectPath")
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    assertNotNull(projectSdk, "project SDK not found for ${project.name} at $projectPath")
    val gradleProjectSettings = GradleProjectSettings()

    GradleSettings.getInstance(project).gradleVmOptions =
        "-Xmx2048m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${System.getProperty("user.dir")}"

    setupGradleSettings(gradleProjectSettings, projectPath, project, projectSdk)
    gradleProjectSettings.gradleJvm = GRADLE_JDK_NAME

    GradleSettings.getInstance(project).getLinkedProjectSettings(projectPath)?.let { linkedProjectSettings ->
        linkedProjectSettings.gradleJvm = GRADLE_JDK_NAME
    }

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
    val settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    if (settings.getLinkedProjectSettings(externalProjectPath) == null) {
        settings.linkProject(gradleProjectSettings)
    }

    StatefulTestGradleProjectRefreshCallback(externalProjectPath, project).use { callback ->
        ExternalSystemUtil.refreshProject(
            externalProjectPath,
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                .use(ProgressExecutionMode.MODAL_SYNC)
                .callback(callback)
                .build()
        )
    }
}
