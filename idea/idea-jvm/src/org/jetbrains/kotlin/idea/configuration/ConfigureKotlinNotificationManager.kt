/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import java.util.concurrent.atomic.AtomicBoolean
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

private val checkInProgress = AtomicBoolean(false)

fun checkHideNonConfiguredNotifications(project: Project) {
    if (checkInProgress.get() || ConfigureKotlinNotificationManager.getVisibleNotifications(project).isEmpty()) return

    ApplicationManager.getApplication().executeOnPooledThread {
        if (!checkInProgress.compareAndSet(false, true)) return@executeOnPooledThread

        DumbService.getInstance(project).waitForSmartMode()
        if (getConfigurableModulesWithKotlinFiles(project).none(::isNotConfiguredNotificationRequired)) {
            ApplicationManager.getApplication().invokeLater {
                ConfigureKotlinNotificationManager.expireOldNotifications(project)
                checkInProgress.set(false)
            }
        }
        else {
            checkInProgress.set(false)
        }
    }
}
