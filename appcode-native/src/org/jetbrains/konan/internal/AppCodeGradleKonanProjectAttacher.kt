/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.internal

import com.intellij.notification.NotificationType
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.service.project.GradleProjectOpenProcessor
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleBundle
import org.jetbrains.plugins.gradle.util.GradleConstants

class AppCodeGradleKonanProjectAttacher(val project: Project) : ProjectComponent {

    override fun projectOpened() {
        if (GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()) {
            val parentDir = "${project.baseDir.parent.path}/"
            val baseDir = "${project.baseDir.path}/"
            val gradleFile = FileUtil.findFirstThatExist(
                baseDir + GradleConstants.DEFAULT_SCRIPT_NAME,
                baseDir + GradleConstants.KOTLIN_DSL_SCRIPT_NAME,
                parentDir + GradleConstants.DEFAULT_SCRIPT_NAME,
                parentDir + GradleConstants.KOTLIN_DSL_SCRIPT_NAME
            )

            if (gradleFile != null) {
                val message = GradleBundle.message("gradle.notifications.unlinked.project.found.msg", "import")

                GradleNotification.getInstance(project).showBalloon(
                    GradleBundle.message("gradle.notifications.unlinked.project.found.title"),
                    message,
                    NotificationType.INFORMATION
                ) { _, event ->
                    if (event.description == "import") {
                        GradleProjectOpenProcessor.attachGradleProjectAndRefresh(project, gradleFile.absolutePath)
                    }
                }
            }
        }
    }
}