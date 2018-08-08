/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
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
)