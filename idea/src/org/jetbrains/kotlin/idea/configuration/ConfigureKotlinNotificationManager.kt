/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import kotlin.reflect.KClass

object ConfigureKotlinNotificationManager: KotlinSingleNotificationManager<ConfigureKotlinNotification> {
    fun notify(project: Project, excludeModules: List<Module> = emptyList()) {
        val notificationString = ConfigureKotlinNotification.getNotificationString(project, excludeModules)
        if (notificationString != null) {
            notify(project, ConfigureKotlinNotification(project, excludeModules, notificationString))
        }
    }

    fun getVisibleNotifications(project: Project): Array<out ConfigureKotlinNotification> {
        return NotificationsManager.getNotificationsManager().getNotificationsOfType(ConfigureKotlinNotification::class.java, project)
    }

    fun expireOldNotifications(project: Project) {
        expireOldNotifications(project, ConfigureKotlinNotification::class)
    }
}

interface KotlinSingleNotificationManager<in T: Notification> {
    fun notify(project: Project, notification: T) {
        if (!expireOldNotifications(project, notification::class, notification)) {
            notification.notify(project)
        }
    }

    fun expireOldNotifications(project: Project, notificationClass: KClass<out T>, notification: T? = null): Boolean {
        val notificationsManager = NotificationsManager.getNotificationsManager()
        var isNotificationExists = false

        val notifications = notificationsManager.getNotificationsOfType(notificationClass.java, project)
        for (oldNotification in notifications) {
            if (oldNotification == notification) {
                isNotificationExists = true
            }
            else {
                oldNotification?.expire()
            }
        }
        return isNotificationExists
    }
}


fun checkHideNonConfiguredNotifications(project: Project) {
    if (ConfigureKotlinNotificationManager.getVisibleNotifications(project).isNotEmpty()) {
        ApplicationManager.getApplication().executeOnPooledThread {
            DumbService.getInstance(project).waitForSmartMode()
            if (getConfigurableModulesWithKotlinFiles(project).all(::isModuleConfigured)) {
                ApplicationManager.getApplication().invokeLater {
                    ConfigureKotlinNotificationManager.expireOldNotifications(project)
                }
            }
        }
    }
}
