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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.configuration.getModulesWithKotlinFiles
import org.jetbrains.kotlin.idea.configuration.notifyOutdatedBundledCompilerIfNecessary
import org.jetbrains.kotlin.idea.configuration.ui.notifications.notifyKotlinStyleUpdateIfNeeded
import org.jetbrains.kotlin.idea.project.getAndCacheLanguageLevelByDependencies
import org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger

class KotlinConfigurationCheckerStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        NotificationsConfiguration.getNotificationsConfiguration()
            .register(
                KotlinConfigurationCheckerService.CONFIGURE_NOTIFICATION_GROUP_ID,
                NotificationDisplayType.STICKY_BALLOON, true
            )

        val connection = project.messageBus.connect(project)
        connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            notifyOutdatedBundledCompilerIfNecessary(project)
        })

        notifyKotlinStyleUpdateIfNeeded(project)

        KotlinConfigurationCheckerService.getInstanceIfNotDisposed(project)?.performProjectPostOpenActions()
    }
}

class KotlinConfigurationCheckerService(val project: Project) {
    private val syncDepth = AtomicInteger()

    fun performProjectPostOpenActions() {
        nonBlocking(Callable {
            return@Callable getModulesWithKotlinFiles(project)
        })
            .inSmartMode(project)
            .expireWith(project)
            .coalesceBy(this)
            .finishOnUiThread(ModalityState.any()) { modulesWithKotlinFiles ->
                val promise = ApplicationManager.getApplication().executeOnPooledThread {
                    for (module in modulesWithKotlinFiles) {
                        runReadAction {
                            module.getAndCacheLanguageLevelByDependencies()
                        }
                    }
                }
                if (isUnitTestMode()) {
                    ProgressIndicatorUtils.awaitWithCheckCanceled(promise)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
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

        fun getInstanceIfNotDisposed(project: Project): KotlinConfigurationCheckerService? {
            return runReadAction {
                if (!project.isDisposed) {
                    project.getService(KotlinConfigurationCheckerService::class.java)
                        ?: error("Can't find ${KotlinConfigurationCheckerService::class} service")
                } else {
                    null
                }
            }
        }
    }
}
