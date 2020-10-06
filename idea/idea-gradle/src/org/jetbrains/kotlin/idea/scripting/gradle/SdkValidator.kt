/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.plugins.gradle.service.project.GradleNotification
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings

class SdkValidator : StartupActivity {
    override fun runActivity(project: Project) {
        GradleSettings.getInstance(project).linkedProjectsSettings.forEach {
            it.validateGradleSdk(project)
        }
    }
}

fun GradleProjectSettings.validateGradleSdk(project: Project, jdkHomePath: String? = null) {
    val gradleJvm = gradleJvm ?: return
    // gradleJvm could be #USE_PROJECT_JDK etc, see ExternalSystemJdkUtil
    val jdk = ExternalSystemJdkUtil.getJdk(project, gradleJvm) ?: return
    val homePath = jdkHomePath ?: jdk?.homePath ?: return
    if (!JdkUtil.checkForJdk(homePath)) {
        GradleNotification.getInstance(project).showBalloon(
            KotlinIdeaGradleBundle.message("notification.invalid.gradle.jvm.configuration.title"),
            KotlinIdeaGradleBundle.message("notification.jdk.0.points.to.invalid.jdk", jdk.name),
            NotificationType.ERROR, null
        )
    }
}