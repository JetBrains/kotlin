/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.getConfigurationPossibilities
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent
import javax.swing.event.HyperlinkEvent

class ConfigureKotlinNotification(
    project: Project,
    excludeModules: List<Module>,
    notificationString: String
) : Notification(
    KotlinConfigurationCheckerComponent.CONFIGURE_NOTIFICATION_GROUP_ID, "Configure Kotlin",
    notificationString,
    NotificationType.WARNING, NotificationListener { notification, event ->
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
        fun getNotificationString(project: Project, excludeModules: Collection<Module>): String? {
            val (configurableModules, ableToRunConfigurators) = getConfigurationPossibilities(project, excludeModules)
            if (ableToRunConfigurators.isEmpty()) return null

            val isOnlyOneModule = configurableModules.size == 1

            val modulesString = if (isOnlyOneModule) "'${configurableModules.first().name}' module" else "modules"
            val links = ableToRunConfigurators.joinToString(separator = "<br/>") { configurator ->
                getLink(configurator, isOnlyOneModule)
            }

            return "Configure $modulesString in '${project.name}' project<br/> $links"
        }

        private fun getLink(configurator: KotlinProjectConfigurator, isOnlyOneModule: Boolean): String {
            return "<a href=\"${configurator.name}\">as Kotlin (${configurator.presentableText}) module${if(!isOnlyOneModule) "s" else ""}</a>"
        }
    }
}
