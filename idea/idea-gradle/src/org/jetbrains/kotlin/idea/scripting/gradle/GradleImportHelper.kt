/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle
import org.jetbrains.kotlin.idea.scripting.gradle.roots.GradleBuildRootsManager
import org.jetbrains.kotlin.idea.util.application.getServiceSafe
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

val scriptConfigurationsNeedToBeUpdatedBalloon
    get() = Registry.`is`("kotlin.gradle.scripts.scriptConfigurationsNeedToBeUpdatedBalloon", false)

fun runPartialGradleImport(project: Project) {
    getGradleProjectSettings(project).forEach {
        ExternalSystemUtil.refreshProject(
            it.externalProjectPath,
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
                .build()
        )
    }
}

fun getMissingConfigurationActionText() = KotlinIdeaGradleBundle.message("action.label.import.project")

fun autoReloadScriptConfigurations(project: Project, file: VirtualFile): Boolean {
    val path = GradleBuildRootsManager.getInstance(project)
        .getScriptInfo(file)?.buildRoot?.pathPrefix ?: return false

    return ExternalSystemApiUtil
        .getSettings(project, GradleConstants.SYSTEM_ID)
        .getLinkedProjectSettings(path)
        ?.isUseAutoImport ?: false
}

private const val kotlinDslNotificationGroupId = "Gradle Kotlin DSL Scripts"
private var Project.notificationPanel: ScriptConfigurationChangedNotification?
        by UserDataProperty<Project, ScriptConfigurationChangedNotification>(Key.create("load.script.configuration.panel"))

fun scriptConfigurationsNeedToBeUpdated(project: Project, file: VirtualFile) {
    if (!scriptConfigurationsNeedToBeUpdatedBalloon) return

    if (autoReloadScriptConfigurations(project, file)) {
        // import should be run automatically by Gradle plugin
        return
    }

    val existingPanel = project.notificationPanel
    if (existingPanel != null) {
        return
    }

    val notificationGroup = NotificationGroup.findRegisteredGroup(kotlinDslNotificationGroupId)
    if (notificationGroup == null) {
        NotificationsConfiguration.getNotificationsConfiguration().register(
            kotlinDslNotificationGroupId, NotificationDisplayType.STICKY_BALLOON, false
        )
    }

    val notification = ScriptConfigurationChangedNotification(project)
    project.notificationPanel = notification
    notification.notify(project)
}

fun scriptConfigurationsAreUpToDate(project: Project): Boolean {
    if (project.notificationPanel == null) return false
    project.notificationPanel?.expire()
    return true
}

private class ScriptConfigurationChangedNotification(val project: Project) :
    Notification(
        kotlinDslNotificationGroupId,
        KotlinIcons.LOAD_SCRIPT_CONFIGURATION,
        KotlinIdeaGradleBundle.message("notification.title.script.configuration.has.been.changed"),
        null,
        KotlinIdeaGradleBundle.message("notification.text.script.configuration.has.been.changed"),
        NotificationType.INFORMATION,
        null
    ) {

    init {
        addAction(LoadConfigurationAction())
        addAction(NotificationAction.createSimple(KotlinIdeaGradleBundle.message("action.label.enable.auto.import")) {
            getGradleProjectSettings(project).forEach {
                it.isUseAutoImport = true
            }
            runPartialGradleImport(project)
        })
    }

    override fun expire() {
        super.expire()

        project.notificationPanel = null
    }

    private class LoadConfigurationAction : AnAction(KotlinIdeaGradleBundle.message("action.label.import.project")) {
        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            runPartialGradleImport(project)
        }
    }
}