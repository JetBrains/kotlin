/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import org.jetbrains.kotlin.idea.configuration.ui.ConfigurePluginUpdatesForm
import javax.swing.JComponent

class KotlinUpdatesSettingsConfigurable() : SearchableConfigurable, Configurable.NoScroll {
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

    private var savedChannel: Int = EAPChannels.EAP_1_3.indexIfAvailable() ?: EAPChannels.EAP_1_2.indexIfAvailable() ?: 0

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Kotlin Updates"

    override fun isModified(): Boolean {
        return savedChannel != form.channelCombo.selectedIndex
    }

    override fun apply() {
        saveSettings()
    }

    override fun createComponent(): JComponent? {
        form.updateCheckProgressIcon.suspend()
        form.updateCheckProgressIcon.setPaintPassiveIcon(false)

        form.checkForUpdatesNowButton.addActionListener {
            saveSettings()
            form.updateCheckProgressIcon.resume()
            form.resetUpdateStatus()
            KotlinPluginUpdater.getInstance().runUpdateCheck{ pluginUpdateStatus ->
                form.updateCheckProgressIcon.suspend()
                when (pluginUpdateStatus) {
                    PluginUpdateStatus.LatestVersionInstalled ->
                        form.updateStatusLabel.text = "You have the latest version of the plugin (${KotlinPluginUtil.getPluginVersion()}) installed."

                    is PluginUpdateStatus.Update -> {
                        update = pluginUpdateStatus
                        form.installButton.isVisible = true
                        form.updateStatusLabel.text = "A new version ${pluginUpdateStatus.pluginDescriptor.version} is available"
                    }

                    is PluginUpdateStatus.CheckFailed ->
                        form.updateStatusLabel.text = "Update check failed: ${pluginUpdateStatus.message}"
                }

                false  // do not auto-retry update check
            }
        }

        form.installButton.isVisible = false
        form.installButton.addActionListener {
            update?.let {
                KotlinPluginUpdater.getInstance().installPluginUpdate(it)
            }
        }

        form.channelCombo.addActionListener {
            form.resetUpdateStatus()
        }

        form.channelCombo.selectedIndex = savedChannel

        return form.mainPanel
    }

    private fun saveSettings() {
        savedChannel = form.channelCombo.selectedIndex
        saveSelectedChannel(savedChannel)
    }
}

