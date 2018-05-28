/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.IdeBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import org.jetbrains.kotlin.idea.configuration.ui.ConfigurePluginUpdatesForm
import javax.swing.JComponent

class KotlinUpdatesSettingsConfigurable : SearchableConfigurable, Configurable.NoScroll {
    companion object {
        const val ID = "preferences.language.Kotlin"

        private fun saveSelectedChannel(channel: Int) {
            val hosts = UpdateSettings.getInstance().storedPluginHosts
            hosts.removeIf {
                it.startsWith("https://plugins.jetbrains.com/plugins/") &&
                        (it.endsWith("/6954") || it.endsWith(KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString))
            }
            when (channel) {
                EAPChannels.EAP_1_3.uiIndex -> hosts.add(EAPChannels.EAP_1_3.url)
                EAPChannels.EAP_1_2.uiIndex -> hosts.add(EAPChannels.EAP_1_2.url)
            }
        }

        enum class EAPChannels(val url: String, val uiIndex: Int) {
            EAP_1_2("https://plugins.jetbrains.com/plugins/eap-1.2/${KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString}", 1),
            EAP_1_3("https://plugins.jetbrains.com/plugins/eap-next/${KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString}", 2);

            private val hasChannel: Boolean get() = url in UpdateSettings.getInstance().pluginHosts

            fun indexIfAvailable() = if (hasChannel) uiIndex else null
        }
    }

    private val form = ConfigurePluginUpdatesForm()
    private var update: PluginUpdateStatus.Update? = null

    private var savedChannel = -1

    private var versionForInstallation: String? = null

    private var installedVersion: String? = null
    private var installingStatus: String? = null

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Kotlin"

    override fun isModified() = false

    override fun apply() {
        // Selected channel is now saved automatically
    }

    private fun setInstalledVersion(installedVersion: String?, installingStatus: String?) {
        this.installedVersion = installedVersion
        this.installingStatus = installingStatus
    }

    override fun createComponent(): JComponent? {
        form.updateCheckProgressIcon.suspend()
        form.updateCheckProgressIcon.setPaintPassiveIcon(false)

        form.reCheckButton.addActionListener {
            checkForUpdates()
        }

        form.installButton.isVisible = false
        form.installButton.addActionListener {
            update?.let {
                form.hideInstallButton()

                setInstalledVersion(it.pluginDescriptor.version, "Installing...")

                form.installStatusLabel.text = installingStatus

                KotlinPluginUpdater.getInstance().installPluginUpdate(
                    it,
                    successCallback = {
                        setInstalledVersion(it.pluginDescriptor.version, IdeBundle.message("plugin.manager.installed.tooltip"))
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = installingStatus
                        }
                    },
                    cancelCallback = {
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = ""
                            form.showInstallButton()

                            setInstalledVersion(null, null)
                        }
                    },
                    errorCallback = {
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = "Installation failed"
                            form.showInstallButton()
                            setInstalledVersion(null, null)
                        }
                    }
                )
            }
        }

        savedChannel = EAPChannels.EAP_1_3.indexIfAvailable() ?: EAPChannels.EAP_1_2.indexIfAvailable() ?: 0
        form.channelCombo.selectedIndex = savedChannel

        form.channelCombo.addActionListener {
            val newChannel = form.channelCombo.selectedIndex
            if (newChannel != savedChannel) {
                savedChannel = newChannel
                checkForUpdates()
            }
        }

        checkForUpdates()

        return form.mainPanel
    }

    private fun checkForUpdates() {
        saveChannelSettings()
        form.updateCheckProgressIcon.resume()
        form.resetUpdateStatus()
        KotlinPluginUpdater.getInstance().runUpdateCheck{ pluginUpdateStatus ->
            // Need this to show something is happening when check is very fast
            Thread.sleep(30)
            form.updateCheckProgressIcon.suspend()

            when (pluginUpdateStatus) {
                PluginUpdateStatus.LatestVersionInstalled -> {
                    form.setUpdateStatus(
                        "You have the latest version of the plugin installed.",
                        false
                    )
                }

                is PluginUpdateStatus.Update -> {
                    update = pluginUpdateStatus
                    versionForInstallation = update?.pluginDescriptor?.version
                    form.setUpdateStatus("A new version ${pluginUpdateStatus.pluginDescriptor.version} is available", true)
                    if (installedVersion != null && installedVersion == versionForInstallation) {
                        // Installation of the plugin has been started or finished
                        form.hideInstallButton()
                        form.installStatusLabel.text = installingStatus
                    }
                }

                is PluginUpdateStatus.CheckFailed ->
                    form.setUpdateStatus("Update check failed: ${pluginUpdateStatus.message}", false)

                is PluginUpdateStatus.Unverified -> {
                    val version = pluginUpdateStatus.updateStatus.pluginDescriptor.version
                    val generalLine = "A new version $version is found but it's not verified by ${pluginUpdateStatus.verifierName}."
                    val reasonLine = pluginUpdateStatus.reason ?: ""
                    val message = "<html>$generalLine<br/>$reasonLine</html>"
                    form.setUpdateStatus(message, false)
                }
            }

            false  // do not auto-retry update check
        }
    }

    private fun saveChannelSettings() {
        saveSelectedChannel(form.channelCombo.selectedIndex)
    }
}

