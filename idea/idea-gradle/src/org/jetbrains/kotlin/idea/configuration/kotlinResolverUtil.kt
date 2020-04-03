/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser
import com.intellij.util.PlatformUtils
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinIdeaGradleBundle

@NonNls
const val NATIVE_DEBUG_ID = "com.intellij.nativeDebug"

fun suggestNativeDebug(projectPath: String) {
    if (!PlatformUtils.isIdeaUltimate() ||
        PluginManagerCore.isPluginInstalled(PluginId.getId(NATIVE_DEBUG_ID))
    ) {
        return
    }

    val project = ProjectManager.getInstance().openProjects.firstOrNull { it.basePath == projectPath } ?: return

    PluginsAdvertiser.NOTIFICATION_GROUP.createNotification(
        KotlinIdeaGradleBundle.message("title.plugin.suggestion"),
        KotlinIdeaGradleBundle.message("notification.text.native.debug.provides.debugger.for.kotlin.native"),
        NotificationType.INFORMATION, null
    ).addAction(object : NotificationAction(KotlinIdeaGradleBundle.message("action.text.install")) {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            PluginsAdvertiser.installAndEnablePlugins(setOf(NATIVE_DEBUG_ID)) { notification.expire() }
        }
    }).notify(project)
}