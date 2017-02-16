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

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationsConfiguration
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.showConfigureKotlinNotificationIfNeeded
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.versions.collectModulesWithOutdatedRuntime
import org.jetbrains.kotlin.idea.versions.findOutdatedKotlinLibraries
import org.jetbrains.kotlin.idea.versions.notifyOutdatedKotlinRuntime
import java.util.concurrent.atomic.AtomicInteger

class KotlinConfigurationCheckerComponent protected constructor(project: Project) : AbstractProjectComponent(project) {
    private var syncCount = AtomicInteger()

    init {
        NotificationsConfiguration.getNotificationsConfiguration().register(CONFIGURE_NOTIFICATION_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)
    }

    override fun projectOpened() {
        super.projectOpened()

        StartupManager.getInstance(myProject).registerPostStartupActivity {
            DumbService.getInstance(myProject).smartInvokeLater {
                for (module in getModulesWithKotlinFiles(myProject)) {
                    module.getAndCacheLanguageLevelByDependencies()
                }

                val libraries = findOutdatedKotlinLibraries(myProject)
                if (!libraries.isEmpty()) {
                    notifyOutdatedKotlinRuntime(myProject, libraries)
                }
                if (syncCount.get() == 0) {
                    showConfigureKotlinNotificationIfNeeded(myProject,
                                                            collectModulesWithOutdatedRuntime(libraries))
                }
            }
        }
    }

    val isSyncing: Boolean get() = syncCount.get() > 0

    fun syncStarted() {
        syncCount.incrementAndGet()
    }

    fun syncDone() {
        if (syncCount.decrementAndGet() == 0) {
            DumbService.getInstance(myProject).smartInvokeLater {
                if (!isSyncing) {
                    showConfigureKotlinNotificationIfNeeded(myProject,
                                                            collectModulesWithOutdatedRuntime(findOutdatedKotlinLibraries(myProject)))
                }
            }
        }
    }

    companion object {
        val CONFIGURE_NOTIFICATION_GROUP_ID = "Configure Kotlin in Project"

        fun getInstance(project: Project): KotlinConfigurationCheckerComponent
                = project.getComponent(KotlinConfigurationCheckerComponent::class.java)
    }
}
