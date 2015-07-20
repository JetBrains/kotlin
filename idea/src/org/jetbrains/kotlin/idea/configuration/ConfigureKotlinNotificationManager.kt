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

import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationsManager
import com.intellij.notification.Notification
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.kotlin.idea.configuration.ui.notifications.*

object ConfigureKotlinNotificationManager: KotlinSingleNotificationManager<ConfigureKotlinNotification> {
    fun notify(project: Project) {
        notify(project, ConfigureKotlinNotification(project, ConfigureKotlinNotification.getNotificationString(project)))
    }
}

@deprecated("Deprecated after moving to platform types")
object AbsentSdkAnnotationsNotificationManager: KotlinSingleNotificationManager<AbsentSdkAnnotationsNotification> {
    fun notify(project: Project, sdks: Collection<Sdk>) {
        notify(project, AbsentSdkAnnotationsNotification(sdks, getNotificationTitle(sdks), getNotificationString(sdks)))
    }
}

trait KotlinSingleNotificationManager<T: Notification> {
    fun notify(project: Project, notification: T) {
        val notificationsManager = NotificationsManager.getNotificationsManager()
        if (notificationsManager == null) {
            return
        }

        var isNotificationExists = false

        val notifications = notificationsManager.getNotificationsOfType(notification.javaClass, project) as Array<Notification>
        for (oldNotification in notifications) {
            if (oldNotification == notification) {
                isNotificationExists = true
            }
            else {
                oldNotification.expire()
            }
        }
        if (!isNotificationExists) {
            notification.notify(project)
        }
    }
}

