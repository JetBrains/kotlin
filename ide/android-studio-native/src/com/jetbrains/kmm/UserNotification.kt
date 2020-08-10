/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

internal class UserNotification(
    private val project: Project
) {
    companion object {
        private const val ID = "KMM notification"
    }

    fun showInfo(
        title: String,
        message: String,
        action: NotificationAction? = null
    ) {
        show(NotificationType.INFORMATION, title, message, action)
    }

    fun showError(
        title: String,
        message: String,
        action: NotificationAction? = null
    ) {
        show(NotificationType.ERROR, title, message, action)
    }

    private fun show(
        type: NotificationType,
        title: String,
        message: String,
        action: NotificationAction? = null
    ) {
        Notification(ID, title, message, type, null).apply {
            action?.let { addAction(it) }
        }.notify(project)
    }
}