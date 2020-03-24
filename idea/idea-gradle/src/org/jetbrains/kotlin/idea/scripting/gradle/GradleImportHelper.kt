/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.rt.coverage.data.ProjectData
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

fun runPartialGradleImport(project: Project) {
    val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val projectSettings = gradleSettings.getLinkedProjectsSettings()
        .filterIsInstance<GradleProjectSettings>()
        .firstOrNull() ?: return

    ExternalSystemUtil.refreshProject(
        projectSettings.externalProjectPath,
        ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .build()
    )
}

private var Project.notificationPanel: Notification?
        by UserDataProperty<Project, Notification>(Key.create("load.script.configuration.panel"))


fun showNotificationForProjectImport(project: Project, callback: () -> Unit) {
    val existingPanel = project.notificationPanel
    if (existingPanel != null) {
        existingPanel.actions.filterIsInstance<LoadConfigurationAction>().forEach { it.onClick = callback }
        return
    }

    val notification = getNotificationGroup().createNotification(
        KotlinIdeaGradleBundle.message("notification.text.script.configuration.has.been.changed"),
        NotificationType.INFORMATION
    )
    notification.addAction(LoadConfigurationAction(callback))
    notification.addAction(NotificationAction.createSimple(KotlinIdeaGradleBundle.message("action.label.enable.auto.import")) {
        val gradleSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val projectSettings = gradleSettings.getLinkedProjectsSettings()
            .filterIsInstance<GradleProjectSettings>()
            .firstOrNull()
        if (projectSettings != null) {
            projectSettings.isUseAutoImport = true
        }
    })
    project.notificationPanel = notification
    notification.notify(project)
}

fun hideNotificationForProjectImport(project: Project): Boolean {
    if (project.notificationPanel == null) return false
    project.notificationPanel?.expire()
    return true
}

private fun getNotificationGroup(): NotificationGroup {
    return NotificationGroup.findRegisteredGroup("Kotlin DSL script configurations")
        ?: NotificationGroup("Kotlin DSL script configurations", NotificationDisplayType.STICKY_BALLOON, true)
}

private class LoadConfigurationAction(var onClick: () -> Unit) : AnAction(KotlinIdeaGradleBundle.message("action.label.import.project")) {
    override fun actionPerformed(e: AnActionEvent) {
        onClick()
    }
}