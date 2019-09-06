/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.internal

import com.intellij.notification.NotificationType
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

class AppCodeGradleKonanProjectAttacher(
    private val project: Project
) : ProjectComponent {

    private val messageBusConnection = project.messageBus.connect()

    init {
        messageBusConnection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            refreshOCRoots()
            val name = XcodeMetaData.getInstance(project).projectOrWorkspaceFile?.nameWithoutExtension
            name?.let { (project as ProjectEx).setProjectName(it) }
        })
    }

    override fun projectOpened() {
        ExternalProjectsManager.getInstance(project).runWhenInitialized {
            if (!GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
                return@runWhenInitialized
            }

            findGradleProject()?.let {
                val message = GradleBundle.message("gradle.notifications.unlinked.project.found.msg", "import")

                GradleNotification.getInstance(project).showBalloon(
                    GradleBundle.message("gradle.notifications.unlinked.project.found.title"),
                    message,
                    NotificationType.INFORMATION
                ) { notification, event ->
                    if (event.description == "import") {
                        notification.expire()
                        GradleProjectOpenProcessor.attachGradleProjectAndRefresh(project, it.absolutePath)
                    }
                }
            }
        }
    }

    private fun findGradleProject(): File? {
        project.guessProjectDir()?.let { projectDir ->
            val baseDir = Paths.get(projectDir.path, "Supporting Files")
            return FileUtil.findFirstThatExist(
                    "$baseDir/${GradleConstants.DEFAULT_SCRIPT_NAME}",
                    "$baseDir/${GradleConstants.KOTLIN_DSL_SCRIPT_NAME}"
            )
        }

        return null
    }

    private fun refreshOCRoots() {
        TransactionGuard.getInstance().submitTransactionAndWait {
            runWriteAction { XcodeMetaData.getInstance(project).updateContentRoots() }
        }
    }

}