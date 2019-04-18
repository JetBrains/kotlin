/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.getConfigurationPossibilitiesForConfigureNotification
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent
import javax.swing.event.HyperlinkEvent

data class ConfigureKotlinNotificationState(
    val debugProjectName: String,
    val notificationString: String,
    val notConfiguredModules: Collection<String>
)

class ConfigureKotlinNotification(
    project: Project,
    excludeModules: List<Module>,
    val notificationState: ConfigureKotlinNotificationState
) : Notification(
    KotlinConfigurationCheckerComponent.CONFIGURE_NOTIFICATION_GROUP_ID, "Configure Kotlin",
    notificationState.notificationString,
    NotificationType.WARNING,
    NotificationListener { notification, event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            val configurator = getConfiguratorByName(event.description) ?: throw AssertionError("Missed action: " + event.description)
            notification.expire()

            configurator.configure(project, excludeModules)
        }
    }
) {
    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is ConfigureKotlinNotification) return false

        if (content != o.content) return false

        return true
    }

    override fun hashCode(): Int {
        return content.hashCode()
    }

    companion object {
        fun getNotificationState(project: Project, excludeModules: Collection<Module>): ConfigureKotlinNotificationState? {
            val (configurableModules, ableToRunConfigurators) = getConfigurationPossibilitiesForConfigureNotification(project, excludeModules)
            if (ableToRunConfigurators.isEmpty() || configurableModules.isEmpty()) return null

            val isOnlyOneModule = configurableModules.size == 1

            val modulesString = if (isOnlyOneModule) "'${configurableModules.first().baseModule.name}' module" else "modules"
            val links = ableToRunConfigurators.joinToString(separator = "<br/>") { configurator ->
                getLink(configurator, isOnlyOneModule)
            }

            return ConfigureKotlinNotificationState(
                project.name,
                "Configure $modulesString in '${project.name}' project<br/> $links",
                configurableModules.map { it.baseModule.name }
            )
        }

        private fun getLink(configurator: KotlinProjectConfigurator, isOnlyOneModule: Boolean): String {
            return "<a href=\"${configurator.name}\">as Kotlin (${configurator.presentableText}) module${if(!isOnlyOneModule) "s" else ""}</a>"
        }
    }
}
