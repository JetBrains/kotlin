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

package org.jetbrains.kotlin.idea.configuration.ui

import com.intellij.ProjectTopics
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.kotlin.idea.configuration.checkHideNonConfiguredNotifications
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.notifyOutdatedBundledCompilerIfNecessary
import org.jetbrains.kotlin.idea.configuration.showConfigureKotlinNotificationIfNeeded
import org.jetbrains.kotlin.idea.configuration.ui.notifications.notifyKotlinStyleUpdateIfNeeded
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.versions.collectModulesWithOutdatedRuntime
import org.jetbrains.kotlin.idea.versions.findOutdatedKotlinLibraries
import java.util.concurrent.atomic.AtomicInteger

class KotlinConfigurationCheckerComponent(val project: Project) : ProjectComponent {
    private val syncDepth = AtomicInteger()

    @Volatile
    private var notificationPostponed = false

    init {
        NotificationsConfiguration.getNotificationsConfiguration()
            .register(CONFIGURE_NOTIFICATION_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                if (!project.isInitialized) return

                if (notificationPostponed && !isSyncing) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        DumbService.getInstance(project).waitForSmartMode()
                        if (!isSyncing) {
                            notificationPostponed = false
                            showConfigureKotlinNotificationIfNeeded(
                                project,
                                collectModulesWithOutdatedRuntime(findOutdatedKotlinLibraries(project))
                            )
                        }
                    }
                }

                checkHideNonConfiguredNotifications(project)
            }
        })

        connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            notifyOutdatedBundledCompilerIfNecessary(project)
        })

        notifyKotlinStyleUpdateIfNeeded(project)
    }

    override fun projectOpened() {
        super.projectOpened()

        StartupManager.getInstance(project).registerPostStartupActivity {
            performProjectPostOpenActions()
        }
    }

    fun performProjectPostOpenActions() {
        ApplicationManager.getApplication().executeOnPooledThread {
            DumbService.getInstance(project).waitForSmartMode()

            for (module in getModulesWithKotlinFiles(project)) {
                module.getAndCacheLanguageLevelByDependencies()
            }

            if (!isSyncing) {
                val libraries = findOutdatedKotlinLibraries(project)
                val excludeModules = collectModulesWithOutdatedRuntime(libraries)
                showConfigureKotlinNotificationIfNeeded(project, excludeModules)
            } else {
                notificationPostponed = true
            }
        }
    }

    val isSyncing: Boolean get() = syncDepth.get() > 0

    fun syncStarted() {
        syncDepth.incrementAndGet()
    }

    fun syncDone() {
        syncDepth.decrementAndGet()
    }

    companion object {
        const val CONFIGURE_NOTIFICATION_GROUP_ID = "Configure Kotlin in Project"

        fun getInstance(project: Project): KotlinConfigurationCheckerComponent =
            project.getComponent(KotlinConfigurationCheckerComponent::class.java)
    }
}
