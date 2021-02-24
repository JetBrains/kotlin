/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.notification.*
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.migration.CodeMigrationAction
import org.jetbrains.kotlin.idea.statistics.MigrationToolFUSCollector

internal fun showMigrationNotification(project: Project, migrationInfo: MigrationInfo) {
    val detectedChangeMessage = buildString {
        appendBr(KotlinBundle.message("configuration.migration.text.detected.migration"))
        if (migrationInfo.oldStdlibVersion != migrationInfo.newStdlibVersion) {
            appendIndentBr(
                KotlinBundle.message(
                    "configuration.migration.text.standard.library",
                    migrationInfo.oldStdlibVersion,
                    migrationInfo.newStdlibVersion
                )
            )
        }

        if (migrationInfo.oldLanguageVersion != migrationInfo.newLanguageVersion) {
            appendIndentBr(
                KotlinBundle.message(
                    "configuration.migration.text.language.version",
                    migrationInfo.oldLanguageVersion,
                    migrationInfo.newLanguageVersion
                )
            )
        }

        if (migrationInfo.oldApiVersion != migrationInfo.newApiVersion) {
            appendIndentBr(
                KotlinBundle.message(
                    "configuration.migration.text.api.version",
                    migrationInfo.oldApiVersion,
                    migrationInfo.newApiVersion
                )
            )
        }
    }

    MigrationToolFUSCollector.logNotification(migrationInfo.oldVersionsToMap())
    KOTLIN_MIGRATION_NOTIFICATION_GROUP
        .createNotification(
            KotlinBundle.message("configuration.migration.title.kotlin.migration"),
            "${KotlinBundle.message("configuration.migration.text.migrations.for.kotlin.code.are.available")}<br/><br/>$detectedChangeMessage",
            NotificationType.WARNING,
            null
        )
        .also { notification ->
            notification.addAction(NotificationAction.createSimple(KotlinBundle.message("configuration.migration.text.run.migrations")) {
                val projectContext = SimpleDataContext.getProjectContext(project)
                val action = ActionManager.getInstance().getAction(CodeMigrationAction.ACTION_ID)
                Notification.fire(notification, action, projectContext)
                MigrationToolFUSCollector.logRun()

                notification.expire()
            })
        }
        .notify(project)
}

private fun StringBuilder.appendBr(line: String) = this.append("$line<br/>")
private fun StringBuilder.appendIndentBr(line: String) = appendBr("&nbsp;&nbsp;$line")

private const val KOTLIN_MIGRATION_NOTIFICATION_ID = "Kotlin Migration"
private val KOTLIN_MIGRATION_NOTIFICATION_GROUP = NotificationGroup(
    KOTLIN_MIGRATION_NOTIFICATION_ID,
    NotificationDisplayType.STICKY_BALLOON,
    true
)
