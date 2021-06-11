/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationType
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class AndroidPluginIncompatibilityCheckerStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        NotificationsConfiguration.getNotificationsConfiguration()
            .register(
                AndroidPluginWarningNotification.ID,
                NotificationDisplayType.STICKY_BALLOON,
                true
            )
        if (PluginManagerCore.getPlugin(PluginId.getId(ANDROID_PLUGIN_ID))?.isEnabled == true) {
            AndroidPluginWarningNotification().notify(project)
        }
    }

    companion object {
        private const val ANDROID_PLUGIN_ID = "org.jetbrains.android"
    }
}

private class AndroidPluginWarningNotification : Notification(
    ID,
    ID,
    "Android Plugin is incompatible with FIR IDE. Please, consider disabling Android plugin. Otherwise, Kotlin resolve may not work.",
    NotificationType.ERROR,
) {
    companion object {
        const val ID = "Android Plugin is incompatible with FIR IDE"
    }
}