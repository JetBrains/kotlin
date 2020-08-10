/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.ios

import com.intellij.execution.RunManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider
import com.intellij.openapi.project.Project
import com.jetbrains.kmm.KmmBundle
import com.jetbrains.kmm.UserNotification
import com.jetbrains.mpp.workspace.ProjectDataServiceBase
import org.jetbrains.kotlin.idea.configuration.KotlinTargetData
import org.jetbrains.kotlin.idea.configuration.readGradleProperty
import java.io.File

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
        val xcodeprojProperty = readGradleProperty(project, XcProjectFile.gradleProperty)
        workspace.locateXCProject(xcodeprojProperty)
        workspace.xcProjectFile?.projectName?.let { projectName ->
            createDefaultAppleRunConfiguration(projectName, project)
        }

        if (xcodeprojProperty == null) {
            val xcFile = project.basePath?.let { XcProjectFile.findXcFile(File(it)) }
            if (xcFile != null) {
                UserNotification(project).showInfo(
                    KmmBundle.message("import.xcfile.suggestion.title"),
                    KmmBundle.message("import.xcfile.suggestion.message", xcFile.path),
                    object : NotificationAction(KmmBundle.message("import.xcfile.suggestion.action")) {
                        override fun actionPerformed(action: AnActionEvent, notification: Notification) {
                            XcProjectFile.setupXcProjectPath(project, xcFile)
                            notification.hideBalloon()
                        }
                    }
                )
            }
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