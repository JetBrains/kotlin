/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.formatter.*
import org.jetbrains.kotlin.idea.util.isDefaultOfficialCodeStyle

private const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"
private const val KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME = "update.kotlin.code.style.notified"

fun notifyKotlinStyleUpdateIfNeeded(project: Project) {
    if (!isDefaultOfficialCodeStyle) return

    val isProjectSettings = CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS
    val settingsComponent: PropertiesComponent = if (isProjectSettings) {
        PropertiesComponent.getInstance(project)
    } else {
        PropertiesComponent.getInstance()
    }

    if (settingsComponent.getBoolean(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, false)) {
        return
    }

    val notification = KotlinCodeStyleChangedNotification.create(project, isProjectSettings) ?: return
    notification.isImportant = true

    NotificationsConfiguration.getNotificationsConfiguration()
        .register(KOTLIN_UPDATE_CODE_STYLE_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    settingsComponent.setValue(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, true, false)

    notification.notify(project)
}

private class KotlinCodeStyleChangedNotification(val project: Project, isProjectSettings: Boolean) : Notification(
    KOTLIN_UPDATE_CODE_STYLE_GROUP_ID,
    "Kotlin Code Style",
    """
        <html>
        Default code style was updated to Kotlin Coding Conventions.
        </html>
        """.trimIndent(),
    NotificationType.WARNING,
    null
) {
    init {
        val ktFormattingSettings = ktCodeStyleSettings(project)

        if (isProjectSettings) {
            addAction(object : NotificationAction("Apply new code style") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        KotlinStyleGuideCodeStyle.apply(ktSettings.all)
                    }
                }
            })
        }

        if (ktFormattingSettings != null && ktFormattingSettings.canRestore()) {
            addAction(object : NotificationAction("Restore old settings") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()

                    val ktSettings = ktCodeStyleSettings(project) ?: return

                    runWriteAction {
                        ktSettings.restore()
                    }
                }
            })
        }
    }

    companion object {
        val LOG = Logger.getInstance("KotlinCodeStyleChangedNotification")

        fun create(project: Project, isProjectSettings: Boolean): KotlinCodeStyleChangedNotification? {
            val ktFormattingSettings = ktCodeStyleSettings(project) ?: return null

            if (isProjectSettings && !ktFormattingSettings.hasDefaultLoadScheme()) {
                return null
            }

            return KotlinCodeStyleChangedNotification(project, isProjectSettings)
        }
    }
}