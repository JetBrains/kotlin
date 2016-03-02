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

import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.plugins.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.Alarm
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class PluginUpdateStatus {
    object LatestVersionInstalled : PluginUpdateStatus()

    class Update(val pluginDescriptor: IdeaPluginDescriptor,
                 val hostToInstallFrom: String?) : PluginUpdateStatus()

    class CheckFailed(val message: String, val detail: String? = null) : PluginUpdateStatus()

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus {
        if (other is Update && (this is LatestVersionInstalled ||
                                (this is Update && VersionComparatorUtil.compare(other.pluginDescriptor.version,
                                                                                 pluginDescriptor.version) > 0))) {
            return other
        }
        return this
    }

    companion object {
        fun fromException(message: String, e: Exception): PluginUpdateStatus {
            val writer = StringWriter()
            e.printStackTrace(PrintWriter(writer))
            return CheckFailed(message, writer.toString())
        }
    }
}

class KotlinPluginUpdater(val propertiesComponent: PropertiesComponent) : Disposable {
    private val INITIAL_UPDATE_DELAY = 5000L
    private var updateDelay = INITIAL_UPDATE_DELAY
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val notificationGroup = NotificationGroup("Kotlin plugin updates",  NotificationDisplayType.STICKY_BALLOON, true)

    @Volatile private var checkQueued = false
    @Volatile private var lastStatus: PluginUpdateStatus? = null

    fun kotlinFileEdited() {
        if (ApplicationManager.getApplication().isUnitTestMode) return
        if (!UpdateSettings.getInstance().isCheckNeeded) return

        val lastUpdateTime = java.lang.Long.parseLong(propertiesComponent.getValue(PROPERTY_NAME, "0"))
        if (lastUpdateTime == 0L || System.currentTimeMillis() - lastUpdateTime > TimeUnit.DAYS.toMillis(1)) {
            queueUpdateCheck { updateStatus ->
                when (updateStatus) {
                    is PluginUpdateStatus.Update -> notifyPluginUpdateAvailable(updateStatus)
                    is PluginUpdateStatus.CheckFailed -> LOG.info("Plugin update check failed: ${updateStatus.message}, details: ${updateStatus.detail}")
                }
                true
            }
        }
    }

    private fun queueUpdateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        if (!checkQueued) {
            checkQueued = true
            alarm.addRequest({ updateCheck(callback) }, updateDelay)
            updateDelay *= 2 // exponential backoff
        }
    }

    fun runUpdateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        ApplicationManager.getApplication().executeOnPooledThread {
            updateCheck(callback)
        }
    }

    private fun updateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        var updateStatus: PluginUpdateStatus
        try {
            updateStatus = checkUpdatesInMainRepository()

            for (host in RepositoryHelper.getPluginHosts().filterNotNull()) {
                val customUpdateStatus = checkUpdatesInCustomRepository(host)
                updateStatus = updateStatus.mergeWith(customUpdateStatus)
            }
        }
        catch(e: Exception) {
            updateStatus = PluginUpdateStatus.fromException("Kotlin plugin update check failed", e)
        }

        lastStatus = updateStatus
        checkQueued = false

        if (updateStatus !is PluginUpdateStatus.CheckFailed) {
            recordSuccessfulUpdateCheck()
        }
        ApplicationManager.getApplication().invokeLater({
            callback(updateStatus)
        }, ModalityState.any())
    }

    private fun initPluginDescriptor(newVersion: String): IdeaPluginDescriptor {
        val originalPlugin = PluginManager.getPlugin(KotlinPluginUtil.KOTLIN_PLUGIN_ID)!!
        return PluginNode(KotlinPluginUtil.KOTLIN_PLUGIN_ID).apply {
            version = newVersion
            name = originalPlugin.name
            description = originalPlugin.description
        }
    }

    private fun checkUpdatesInMainRepository(): PluginUpdateStatus {
        val buildNumber = ApplicationInfo.getInstance().build.asString()
        val currentVersion = KotlinPluginUtil.getPluginVersion()
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
        val uid = UpdateChecker.getInstallationUID(propertiesComponent)
        val url = "https://plugins.jetbrains.com/plugins/list?pluginId=6954&build=$buildNumber&pluginVersion=$currentVersion&os=$os&uuid=$uid"
        val responseDoc = HttpRequests.request(url).connect {
            JDOMUtil.load(it.inputStream)
        }
        if (responseDoc.name != "plugin-repository") {
            return PluginUpdateStatus.CheckFailed("Unexpected plugin repository response", JDOMUtil.writeElement(responseDoc, "\n"))
        }
        if (responseDoc.children.isEmpty()) {
            // No plugin version compatible with current IDEA build; don't retry updates
            return PluginUpdateStatus.LatestVersionInstalled
        }
        val newVersion = responseDoc.getChild("category")?.getChild("idea-plugin")?.getChild("version")?.text
        if (newVersion == null) {
            return PluginUpdateStatus.CheckFailed("Couldn't find plugin version in repository response", JDOMUtil.writeElement(responseDoc, "\n"))
        }
        val pluginDescriptor = initPluginDescriptor(newVersion)
         return updateIfNotLatest(pluginDescriptor, null)
    }

    private fun checkUpdatesInCustomRepository(host: String): PluginUpdateStatus {
        val plugins = try {
            RepositoryHelper.loadPlugins(host, null)
        }
        catch(e: Exception) {
            return PluginUpdateStatus.fromException("Checking custom plugin repository $host failed", e)
        }

        val kotlinPlugin = plugins.find { it.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID } ?: return PluginUpdateStatus.LatestVersionInstalled
        return updateIfNotLatest(kotlinPlugin, host)
    }

    private fun updateIfNotLatest(kotlinPlugin: IdeaPluginDescriptor, host: String?): PluginUpdateStatus {
        if (VersionComparatorUtil.compare(kotlinPlugin.version, KotlinPluginUtil.getPluginVersion()) <= 0) {
            return PluginUpdateStatus.LatestVersionInstalled
        }

        return PluginUpdateStatus.Update(kotlinPlugin, host)
    }

    private fun recordSuccessfulUpdateCheck() {
        propertiesComponent.setValue(PROPERTY_NAME, System.currentTimeMillis().toString())
        updateDelay = INITIAL_UPDATE_DELAY
    }

    private fun notifyPluginUpdateAvailable(update: PluginUpdateStatus.Update) {
        val notification = notificationGroup.createNotification(
                "Kotlin",
                "A new version ${update.pluginDescriptor.version} of the Kotlin plugin is available. <b><a href=\"#\">Install</a></b>",
                NotificationType.INFORMATION) { notification, event ->
            notification.expire()
            installPluginUpdate(update) {
                notifyPluginUpdateAvailable(update)
            }
        }

        notification.notify(null)
    }

    fun installPluginUpdate(update: PluginUpdateStatus.Update,
                            cancelCallback: () -> Unit = {}) {
        val descriptor = update.pluginDescriptor
        val pluginDownloader = PluginDownloader.createDownloader(descriptor, update.hostToInstallFrom, null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Downloading plugins", true) {
            override fun run(indicator: ProgressIndicator) {
                var installed = false
                if (pluginDownloader.prepareToInstall(indicator)) {
                    val pluginDescriptor = pluginDownloader.descriptor
                    if (pluginDescriptor != null) {
                        installed = true
                        pluginDownloader.install()

                        ApplicationManager.getApplication().invokeLater {
                            PluginManagerMain.notifyPluginsUpdated(null)
                        }
                    }
                }

                if (!installed) {
                    notifyNotInstalled()
                }
            }

            override fun onCancel() {
                cancelCallback()
            }
        })
    }

    private fun notifyNotInstalled() {
        ApplicationManager.getApplication().invokeLater {
            val notification = notificationGroup.createNotification(
                    "Kotlin",
                    "Plugin update was not installed. <a href=\"#\">See the log for more information</a>",
                    NotificationType.INFORMATION) { notification, event ->

                val logFile = File(PathManager.getLogPath(), "idea.log")
                ShowFilePathAction.openFile(logFile)

                notification.expire()
            }

            notification.notify(null)
        }
    }

    override fun dispose() {
    }

    companion object {
        private val PROPERTY_NAME = "kotlin.lastUpdateCheck"
        private val LOG = Logger.getInstance(KotlinPluginUpdater::class.java)

        fun getInstance(): KotlinPluginUpdater = ServiceManager.getService(KotlinPluginUpdater::class.java)
    }
}
