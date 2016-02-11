/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import java.util.*

open class NotificationMessageCollector(private val project: Project,
                                        private val groupDisplayId: String,
                                        private val title: String) {
    private val messages = ArrayList<String>()

    fun addMessage(message: String): NotificationMessageCollector {
        messages.add(message)
        return this
    }

    fun showNotification() {
        if (messages.isEmpty()) return
        Notifications.Bus.notify(Notification(groupDisplayId, title, resultMessage, NotificationType.INFORMATION), project)
    }

    private val resultMessage: String get() {
        val singleMessage = messages.singleOrNull()
        if (singleMessage != null) return singleMessage

        return messages.joinToString(separator = "<br/><br/>")
    }
}

fun createConfigureKotlinNotificationCollector(project: Project) =
        NotificationMessageCollector(project, "Configure Kotlin: info notification", "Configure Kotlin")