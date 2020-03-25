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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.notifyOutdatedBundledCompilerIfNecessary
import org.jetbrains.kotlin.idea.configuration.ui.notifications.notifyKotlinStyleUpdateIfNeeded
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import java.util.concurrent.atomic.AtomicInteger

class KotlinConfigurationCheckerComponent(val project: Project) : ProjectComponent {
    private val syncDepth = AtomicInteger()

    init {
        NotificationsConfiguration.getNotificationsConfiguration()
            .register(CONFIGURE_NOTIFICATION_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true)

        val connection = project.messageBus.connect()
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
            val modulesWithKotlinFiles = project.runReadActionInSmartMode {
                getModulesWithKotlinFiles(project)
            }
            for (module in modulesWithKotlinFiles) {
                runReadAction {
                    if (project.isDisposed) return@runReadAction
                    module.getAndCacheLanguageLevelByDependencies()
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
        const val CONFIGURE_NOTIFICATION_GROUP_ID = "Configure Kotlin in Project"

        fun getInstanceIfNotDisposed(project: Project): KotlinConfigurationCheckerComponent? {
            return runReadAction {
                if (!project.isDisposed) {
                    project.getComponent(KotlinConfigurationCheckerComponent::class.java)
                        ?: error("Can't find ${KotlinConfigurationCheckerComponent::class} component")
                } else {
                    null
                }
            }
        }
    }
}
