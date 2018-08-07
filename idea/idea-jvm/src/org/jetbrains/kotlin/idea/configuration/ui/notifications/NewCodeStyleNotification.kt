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
import org.jetbrains.kotlin.idea.formatter.canRestore
import org.jetbrains.kotlin.idea.formatter.ktCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.restore
import org.jetbrains.kotlin.idea.util.isDefaultOfficialCodeStyle

private const val KOTLIN_UPDATE_CODE_STYLE_GROUP_ID = "Update Kotlin code style"
private const val KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME = "update.kotlin.code.style.notified"

fun notifyKotlinStyleUpdateIfNeeded(project: Project) {
    if (PropertiesComponent.getInstance(project).getBoolean(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, false)) {
        return
    }

    if (!isDefaultOfficialCodeStyle) return

    NotificationsConfiguration.getNotificationsConfiguration()
        .register(KOTLIN_UPDATE_CODE_STYLE_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

    val notification = KotlinCodeStyleChangedNotification(project)
    notification.isImportant = true

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    PropertiesComponent.getInstance(project).setValue(KOTLIN_UPDATE_CODE_STYLE_PROPERTY_NAME, true, false)

    notification.notify(project)
}

private class KotlinCodeStyleChangedNotification(val project: Project) : Notification(
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
        if (ktFormattingSettings != null && ktFormattingSettings.canRestore()) {
            addAction(object : NotificationAction("Restore old settings") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    restore(project)
                }
            })
        }
    }

    companion object {
        val LOG = Logger.getInstance("KotlinCodeStyleChangedNotification")

        private fun restore(project: Project) {
            val ktSettings = ktCodeStyleSettings(project) ?: return

            runWriteAction {
                ktSettings.restore()
            }
        }
    }
}