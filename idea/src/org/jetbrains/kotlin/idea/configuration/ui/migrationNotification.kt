/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.migration.CodeMigrationAction
import org.jetbrains.kotlin.idea.migration.CodeMigrationToggleAction

internal fun showMigrationNotification(project: Project, migrationInfo: MigrationInfo) {
    val detectedChangeMessage = buildString {
        appendBr("Detected migration:")
        if (migrationInfo.oldStdlibVersion != migrationInfo.newStdlibVersion) {
            appendIndentBr("Standard library: ${migrationInfo.oldStdlibVersion} -> ${migrationInfo.newStdlibVersion}")
        }

        if (migrationInfo.oldLanguageVersion != migrationInfo.newLanguageVersion) {
            appendIndentBr("Language version: ${migrationInfo.oldLanguageVersion} -> ${migrationInfo.newLanguageVersion}")
        }

        if (migrationInfo.oldApiVersion != migrationInfo.newApiVersion) {
            appendIndentBr("API version: ${migrationInfo.oldApiVersion} -> ${migrationInfo.newApiVersion}")
        }
    }

    KOTLIN_MIGRATION_NOTIFICATION_GROUP
        .createNotification(
            KOTLIN_MIGRATION_NOTIFICATION_ID,
            "Migrations for Kotlin code are available<br/><br/>$detectedChangeMessage",
            NotificationType.WARNING,
            null
        )
        .also { notification ->
            notification.addAction(NotificationAction.createSimple("Run migrations") {
                val projectContext = SimpleDataContext.getProjectContext(project)
                val action = ActionManager.getInstance().getAction(CodeMigrationAction.ACTION_ID)
                Notification.fire(notification, action, projectContext)

                notification.expire()
            })
        }
        .notify(project)
}

private fun StringBuilder.appendBr(line: String) = this.append("$line<br/>")
private fun StringBuilder.appendIndentBr(line: String) = appendBr("&nbsp;&nbsp;$line")

private const val KOTLIN_MIGRATION_NOTIFICATION_ID = "Kotlin Migration"
private val KOTLIN_MIGRATION_NOTIFICATION_GROUP =
    NotificationGroup(KOTLIN_MIGRATION_NOTIFICATION_ID, NotificationDisplayType.STICKY_BALLOON, true)
