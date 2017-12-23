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
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.kotlin.idea.configuration.checkHideNonConfiguredNotifications
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.showConfigureKotlinNotificationIfNeeded
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.versions.collectModulesWithOutdatedRuntime
import org.jetbrains.kotlin.idea.versions.findOutdatedKotlinLibraries
import org.jetbrains.kotlin.idea.versions.notifyOutdatedKotlinRuntime
import java.util.concurrent.atomic.AtomicInteger

class KotlinConfigurationCheckerComponent(project: Project) : AbstractProjectComponent(project) {
    private val syncDepth = AtomicInteger()
    @Volatile private var notificationPostponed = false

    init {
        NotificationsConfiguration.getNotificationsConfiguration().register(CONFIGURE_NOTIFICATION_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

        val connection = project.messageBus.connect()
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent?) {
                if (notificationPostponed && !isSyncing) {
                    ApplicationManager.getApplication().executeOnPooledThread {
                        DumbService.getInstance(myProject).waitForSmartMode()
                        if (!isSyncing) {
                            notificationPostponed = false
                            showConfigureKotlinNotificationIfNeeded(myProject,
                                                                    collectModulesWithOutdatedRuntime(findOutdatedKotlinLibraries(myProject)))
                        }
                    }
                }

                checkHideNonConfiguredNotifications(project)
            }
        })
    }

    override fun projectOpened() {
        super.projectOpened()

        StartupManager.getInstance(myProject).registerPostStartupActivity {
            ApplicationManager.getApplication().executeOnPooledThread {
                DumbService.getInstance(myProject).waitForSmartMode()

                for (module in getModulesWithKotlinFiles(myProject)) {
                    module.getAndCacheLanguageLevelByDependencies()
                }

                val libraries = findOutdatedKotlinLibraries(myProject)
                if (!libraries.isEmpty()) {
                    ApplicationManager.getApplication().invokeLater {
                        notifyOutdatedKotlinRuntime(myProject, libraries)
                    }
                }
                if (!isSyncing) {
                    val excludeModules = collectModulesWithOutdatedRuntime(libraries)
                    showConfigureKotlinNotificationIfNeeded(myProject, excludeModules)
                }
                else {
                    notificationPostponed = true
                }
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
        val CONFIGURE_NOTIFICATION_GROUP_ID = "Configure Kotlin in Project"

        fun getInstance(project: Project): KotlinConfigurationCheckerComponent
                = project.getComponent(KotlinConfigurationCheckerComponent::class.java)
    }
}
