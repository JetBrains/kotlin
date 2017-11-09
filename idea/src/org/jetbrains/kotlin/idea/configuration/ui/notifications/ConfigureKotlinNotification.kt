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

package org.jetbrains.kotlin.idea.configuration.ui.notifications

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator
import org.jetbrains.kotlin.idea.configuration.getAbleToRunConfigurators
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.configuration.getCanBeConfiguredModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.ui.KotlinConfigurationCheckerComponent
import javax.swing.event.HyperlinkEvent

class ConfigureKotlinNotification(
        project: Project,
        excludeModules: List<Module>,
        notificationString: String) : Notification(KotlinConfigurationCheckerComponent.CONFIGURE_NOTIFICATION_GROUP_ID, "Configure Kotlin",
                                                   notificationString,
                                                   NotificationType.WARNING, NotificationListener { notification, event ->
    if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        val configurator = getConfiguratorByName(event.description) ?: throw AssertionError("Missed action: " + event.description)
        notification.expire()

        configurator.configure(project, excludeModules)
    }
}) {

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
            val modules = getCanBeConfiguredModulesWithKotlinFiles(project, excludeModules)

            val isOnlyOneModule = modules.size == 1

            val modulesString = if (isOnlyOneModule) "'${modules.first().name}' module" else "modules"
            val ableToRunConfigurators = getAbleToRunConfigurators(project)
            if (ableToRunConfigurators.isEmpty()) return null
            val links = ableToRunConfigurators.joinToString(separator = "<br/>") {
                configurator -> getLink(configurator, isOnlyOneModule)
            }

            return "Configure $modulesString in '${project.name}' project<br/> $links"
        }

        private fun getLink(configurator: KotlinProjectConfigurator, isOnlyOneModule: Boolean): String {
            return "<a href=\"${configurator.name}\">as Kotlin (${configurator.presentableText}) module${if(!isOnlyOneModule) "s" else ""}</a>"
        }
    }
}
