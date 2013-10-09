/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.configuration

import com.intellij.openapi.project.Project
import org.jetbrains.jet.plugin.configuration.ui.ConfigureKotlinNotification
import com.intellij.notification.NotificationsManager

object ConfigureKotlinNotificationManager {
    fun notify(project: Project) {
        val notificationsManager = NotificationsManager.getNotificationsManager()
        if (notificationsManager == null) {
            return
        }

        val notificationString = ConfigureKotlinNotification.getNotificationString(project)

        var isNotificationExists = false

        val notifications = notificationsManager.getNotificationsOfType(javaClass<ConfigureKotlinNotification>(), project)
        for (oldNotification in notifications) {
            if (oldNotification.getNotificationText() == notificationString) {
                isNotificationExists = true
            }
            else {
                oldNotification.expire()
            }
        }
        if (!isNotificationExists) {
            ConfigureKotlinNotification(project, notificationString).showNotification()
        }
    }
}

