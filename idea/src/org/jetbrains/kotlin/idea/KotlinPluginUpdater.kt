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

package org.jetbrains.kotlin.idea

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerMain
import com.intellij.ide.plugins.RepositoryHelper
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.Alarm
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class KotlinPluginUpdater(val propertiesComponent: PropertiesComponent) : Disposable {
    private val INITIAL_UPDATE_DELAY = 5000L
    private var updateDelay = INITIAL_UPDATE_DELAY
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    @Volatile private var checkQueued = false
    private val notificationGroup = NotificationGroup("Kotlin plugin updates",  NotificationDisplayType.STICKY_BALLOON, true)

    fun kotlinFileEdited() {
        val lastUpdateTime = java.lang.Long.parseLong(propertiesComponent.getValue(PROPERTY_NAME, "0"))
        if (lastUpdateTime == 0L || System.currentTimeMillis() - lastUpdateTime > TimeUnit.DAYS.toMillis(1)) {
            queueUpdateCheck()
        }
    }

    private fun queueUpdateCheck() {
        if (ApplicationManager.getApplication().isUnitTestMode) return

        ApplicationManager.getApplication().assertIsDispatchThread()
        if (!checkQueued) {
            checkQueued = true
            alarm.addRequest({ updateCheck() }, updateDelay)
            updateDelay *= 2 // exponential backoff
        }
    }

    private fun updateCheck() {
        try {
            var (mainRepoUpdateSuccess, latestVersionInRepository) = getPluginVersionFromMainRepository()
            var descriptorToInstall: IdeaPluginDescriptor? = null
            var hostToInstallFrom: String? = null

            for (host in RepositoryHelper.getPluginHosts().filterNotNull()) {
                val plugins = try {
                    RepositoryHelper.loadPlugins(host, null)
                }
                catch(e: Exception) {
                    LOG.info("Checking custom plugin reposityory $host failed", e)
                    continue
                }

                val kotlinPlugin = plugins.find { it.pluginId.toString() == "org.jetbrains.kotlin" }
                if (kotlinPlugin != null && VersionComparatorUtil.compare(kotlinPlugin.version, latestVersionInRepository) > 0) {
                    latestVersionInRepository = kotlinPlugin.version
                    descriptorToInstall = kotlinPlugin
                    hostToInstallFrom = host
                }
            }

            checkQueued = false

            if (mainRepoUpdateSuccess || latestVersionInRepository != null) {
                recordSuccessfulUpdateCheck()
                if (latestVersionInRepository != null && VersionComparatorUtil.compare(latestVersionInRepository, KotlinPluginUtil.getPluginVersion()) > 0) {
                    ApplicationManager.getApplication().invokeLater {
                        notifyPluginUpdateAvailable(latestVersionInRepository!!, descriptorToInstall, hostToInstallFrom)
                    }
                }
            }
            else {
                ApplicationManager.getApplication().invokeLater { queueUpdateCheck() }
            }
        }
        catch(e: Exception) {
            LOG.info("Kotlin plugin update check failed", e)
            checkQueued = false
            queueUpdateCheck()
        }
    }


    data class RepositoryCheckResult(val success: Boolean, val newVersion: String?)

    fun getPluginVersionFromMainRepository(): RepositoryCheckResult {
        val buildNumber = ApplicationInfo.getInstance().build.asString()
        val pluginVersion = KotlinPluginUtil.getPluginVersion()
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
        val uid = UpdateChecker.getInstallationUID(propertiesComponent)
        val url = "https://plugins.jetbrains.com/plugins/list?pluginId=6954&build=$buildNumber&pluginVersion=$pluginVersion&os=$os&uuid=a$uid"
        val responseDoc = HttpRequests.request(url).connect {
            JDOMUtil.load(it.inputStream)
        }
        if (responseDoc.name != "plugin-repository") {
            LOG.info("Unexpected plugin repository response: ${JDOMUtil.writeElement(responseDoc, "\n")}")
            return RepositoryCheckResult(false, null)
        }
        if (responseDoc.children.isEmpty()) {
            // No plugin version compatible with current IDEA build; don't retry updates
            return RepositoryCheckResult(true, null)
        }
        val version = responseDoc.getChild("category")?.getChild("idea-plugin")?.getChild("version")?.text
        if (version == null) {
            LOG.info("Couldn't find plugin version in repository response: ${JDOMUtil.writeElement(responseDoc, "\n")}")
            return RepositoryCheckResult(false, null)
        }
        return RepositoryCheckResult(true, version)
    }

    private fun recordSuccessfulUpdateCheck() {
        propertiesComponent.setValue(PROPERTY_NAME, System.currentTimeMillis().toString())
        updateDelay = INITIAL_UPDATE_DELAY
    }

    private fun notifyPluginUpdateAvailable(newVersion: String,
                                            descriptorToInstall: IdeaPluginDescriptor?,
                                            hostToInstallFrom: String?) {
        val notification = notificationGroup.createNotification(
                "Kotlin",
                "A new version $newVersion of the Kotlin plugin is available. <b><a href=\"#\">Install</a></b>",
                NotificationType.INFORMATION) { notification, event ->
            val descriptor = descriptorToInstall ?: PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"))
            if (descriptor != null) {
                notification.expire()

                val pluginDownloader = PluginDownloader.createDownloader(descriptor, hostToInstallFrom, null)
                ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Downloading plugins", true) {
                    override fun run(indicator: ProgressIndicator) {
                        if (pluginDownloader.prepareToInstall(indicator)) {
                            val pluginDescriptor = pluginDownloader.getDescriptor()
                            if (pluginDescriptor != null) {
                                pluginDownloader.install()

                                ApplicationManager.getApplication().invokeLater {
                                    notification.expire()
                                    PluginManagerMain.notifyPluginsUpdated(null)
                                }
                            }
                        }
                    }

                    override fun onCancel() {
                        notifyPluginUpdateAvailable(newVersion, descriptorToInstall, hostToInstallFrom)
                    }
                })
            }
        }

        notification.notify(null)
    }

    override fun dispose() {
    }

    companion object {
        private val PROPERTY_NAME = "kotlin.lastUpdateCheck"
        private val LOG = Logger.getInstance(KotlinPluginUpdater::class.java)

        fun getInstance(): KotlinPluginUpdater = ServiceManager.getService(KotlinPluginUpdater::class.java)
    }
}
