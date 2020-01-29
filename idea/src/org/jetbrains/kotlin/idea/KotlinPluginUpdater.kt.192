/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.ide.plugins.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import com.intellij.util.io.HttpRequests
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.update.verify
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.net.URLEncoder
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

sealed class PluginUpdateStatus {
    val timestamp = System.currentTimeMillis()

    object LatestVersionInstalled : PluginUpdateStatus()

    class Update(
        val pluginDescriptor: IdeaPluginDescriptor,
        val hostToInstallFrom: String?
    ) : PluginUpdateStatus()

    class CheckFailed(val message: String, val detail: String? = null) : PluginUpdateStatus()

    class Unverified(val verifierName: String, val reason: String?, val updateStatus: Update) : PluginUpdateStatus()

    fun mergeWith(other: PluginUpdateStatus): PluginUpdateStatus {
        if (other is Update) {
            when (this) {
                is LatestVersionInstalled -> {
                    return other
                }
                is Update -> {
                    if (VersionComparatorUtil.compare(other.pluginDescriptor.version, pluginDescriptor.version) > 0) {
                        return other
                    }
                }
                is CheckFailed, is Unverified -> {
                    // proceed to return this
                }
            }
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

class KotlinPluginUpdater : Disposable {
    private var updateDelay = INITIAL_UPDATE_DELAY
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val notificationGroup = NotificationGroup("Kotlin plugin updates", NotificationDisplayType.STICKY_BALLOON, true)

    @Volatile
    private var checkQueued = false

    @Volatile
    private var lastUpdateStatus: PluginUpdateStatus? = null

    fun kotlinFileEdited(file: VirtualFile) {
        if (!file.isInLocalFileSystem) return
        if (ApplicationManager.getApplication().isUnitTestMode || ApplicationManager.getApplication().isHeadlessEnvironment) return
        if (!UpdateSettings.getInstance().isCheckNeeded) return

        val lastUpdateTime = java.lang.Long.parseLong(PropertiesComponent.getInstance().getValue(PROPERTY_NAME, "0"))
        if (lastUpdateTime == 0L || System.currentTimeMillis() - lastUpdateTime > CACHED_REQUEST_DELAY) {
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

    fun runCachedUpdate(callback: (PluginUpdateStatus) -> Boolean) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val cachedStatus = lastUpdateStatus
        if (cachedStatus != null && System.currentTimeMillis() - cachedStatus.timestamp < CACHED_REQUEST_DELAY) {
            if (cachedStatus !is PluginUpdateStatus.CheckFailed) {
                callback(cachedStatus)
                return
            }
        }

        queueUpdateCheck(callback)
    }

    private fun updateCheck(callback: (PluginUpdateStatus) -> Boolean) {
        var updateStatus: PluginUpdateStatus
        if (KotlinPluginUtil.isSnapshotVersion() || KotlinPluginUtil.isPatched()) {
            updateStatus = PluginUpdateStatus.LatestVersionInstalled
        } else {
            try {
                updateStatus = checkUpdatesInMainRepository()

                for (host in RepositoryHelper.getPluginHosts().filterNotNull()) {
                    val customUpdateStatus = checkUpdatesInCustomRepository(host)
                    updateStatus = updateStatus.mergeWith(customUpdateStatus)
                }
            } catch (e: Exception) {
                updateStatus = PluginUpdateStatus.fromException("Kotlin plugin update check failed", e)
            }
        }

        lastUpdateStatus = updateStatus
        checkQueued = false

        if (updateStatus is PluginUpdateStatus.Update) {
            updateStatus = verify(updateStatus)
        }

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
        val buildNumber = ApplicationInfo.getInstance().apiVersion
        val currentVersion = KotlinPluginUtil.getPluginVersion()
        val os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8)
        val uid = PermanentInstallationID.get()
        val pluginId = KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString
        val url =
            "https://plugins.jetbrains.com/plugins/list?pluginId=$pluginId&build=$buildNumber&pluginVersion=$currentVersion&os=$os&uuid=$uid"
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
            ?: return PluginUpdateStatus.CheckFailed(
                "Couldn't find plugin version in repository response",
                JDOMUtil.writeElement(responseDoc, "\n")
            )
        val pluginDescriptor = initPluginDescriptor(newVersion)
        return updateIfNotLatest(pluginDescriptor, null)
    }

    private fun checkUpdatesInCustomRepository(host: String): PluginUpdateStatus {
        val plugins = try {
            RepositoryHelper.loadPlugins(host, null)
        } catch (e: Exception) {
            return PluginUpdateStatus.fromException("Checking custom plugin repository $host failed", e)
        }

        val kotlinPlugin = plugins.find { pluginDescriptor ->
            pluginDescriptor.pluginId == KotlinPluginUtil.KOTLIN_PLUGIN_ID && PluginManagerCore.isCompatible(pluginDescriptor)
        } ?: return PluginUpdateStatus.LatestVersionInstalled

        return updateIfNotLatest(kotlinPlugin, host)
    }

    private fun updateIfNotLatest(kotlinPlugin: IdeaPluginDescriptor, host: String?): PluginUpdateStatus {
        if (VersionComparatorUtil.compare(kotlinPlugin.version, KotlinPluginUtil.getPluginVersion()) <= 0) {
            return PluginUpdateStatus.LatestVersionInstalled
        }

        return PluginUpdateStatus.Update(kotlinPlugin, host)
    }

    private fun recordSuccessfulUpdateCheck() {
        PropertiesComponent.getInstance().setValue(PROPERTY_NAME, System.currentTimeMillis().toString())
        updateDelay = INITIAL_UPDATE_DELAY
    }

    private fun notifyPluginUpdateAvailable(update: PluginUpdateStatus.Update) {
        val notification = notificationGroup.createNotification(
            "Kotlin",
            "A new version ${update.pluginDescriptor.version} of the Kotlin plugin is available. <b><a href=\"#\">Install</a></b>",
            NotificationType.INFORMATION,
            NotificationListener { notification, _ ->
                notification.expire()
                installPluginUpdate(update) {
                    notifyPluginUpdateAvailable(update)
                }
            })

        notification.notify(null)
    }

    fun installPluginUpdate(
        update: PluginUpdateStatus.Update,
        successCallback: () -> Unit = {}, cancelCallback: () -> Unit = {}, errorCallback: () -> Unit = {}
    ) {
        val descriptor = update.pluginDescriptor
        val pluginDownloader = PluginDownloader.createDownloader(descriptor, update.hostToInstallFrom, null)
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null, "Downloading plugins", true, PluginManagerUISettings.getInstance()
        ) {
            override fun run(indicator: ProgressIndicator) {
                var installed = false
                var message: String? = null
                val prepareResult = try {
                    pluginDownloader.prepareToInstall(indicator)
                } catch (e: IOException) {
                    LOG.info(e)
                    message = e.message
                    false
                }

                if (prepareResult) {
                    installed = true
                    pluginDownloader.install()

                    ApplicationManager.getApplication().invokeLater {
                        PluginManagerMain.notifyPluginsUpdated(null)
                    }
                }

                ApplicationManager.getApplication().invokeLater {
                    if (!installed) {
                        errorCallback()
                        notifyNotInstalled(message)
                    } else {
                        successCallback()
                    }
                }
            }

            override fun onCancel() {
                cancelCallback()
            }
        })
    }

    private fun notifyNotInstalled(message: String?) {
        val fullMessage = message?.let { ": $it" } ?: ""
        val notification = notificationGroup.createNotification(
            "Kotlin",
            "Plugin update was not installed$fullMessage. <a href=\"#\">See the log for more information</a>",
            NotificationType.INFORMATION,
            NotificationListener { notification, _ ->
                val logFile = File(PathManager.getLogPath(), "idea.log")
                ShowFilePathAction.openFile(logFile)

                notification.expire()
            }
        )

        notification.notify(null)
    }

    override fun dispose() {
    }

    companion object {
        private const val INITIAL_UPDATE_DELAY = 2000L
        private val CACHED_REQUEST_DELAY = TimeUnit.DAYS.toMillis(1)

        private const val PROPERTY_NAME = "kotlin.lastUpdateCheck"
        private val LOG = Logger.getInstance(KotlinPluginUpdater::class.java)

        fun getInstance(): KotlinPluginUpdater = ServiceManager.getService(KotlinPluginUpdater::class.java)

        class ResponseParseException(message: String, cause: Exception? = null) : IllegalStateException(message, cause)

        @Suppress("SpellCheckingInspection")
        private class PluginDTO {
            var cdate: String? = null
            var channel: String? = null

            // `true` if the version is seen in plugin site and available for download.
            // Maybe be `false` if author requested version deletion.
            var listed: Boolean = true

            // `true` if version is approved and verified
            var approve: Boolean = true
        }

        @Throws(IOException::class, ResponseParseException::class)
        fun fetchPluginReleaseDate(pluginId: PluginId, version: String, channel: String?): LocalDate? {
            val url = "https://plugins.jetbrains.com/api/plugins/${pluginId.idString}/updates?version=$version"

            val pluginDTOs: Array<PluginDTO> = try {
                HttpRequests.request(url).connect {
                    GsonBuilder().create().fromJson(it.inputStream.reader(), Array<PluginDTO>::class.java)
                }
            } catch (ioException: JsonIOException) {
                throw IOException(ioException)
            } catch (syntaxException: JsonSyntaxException) {
                throw ResponseParseException("Can't parse json response", syntaxException)
            }

            val selectedPluginDTO = pluginDTOs
                .firstOrNull {
                    it.listed && it.approve && (it.channel == channel || (it.channel == "" && channel == null))
                }
                ?: return null

            val dateString = selectedPluginDTO.cdate ?: throw ResponseParseException("Empty cdate")

            return try {
                val dateLong = dateString.toLong()
                Instant.ofEpochMilli(dateLong).atZone(ZoneOffset.UTC).toLocalDate()
            } catch (e: NumberFormatException) {
                throw ResponseParseException("Can't parse long date", e)
            } catch (e: DateTimeException) {
                throw ResponseParseException("Can't convert to date", e)
            }
        }
    }
}
