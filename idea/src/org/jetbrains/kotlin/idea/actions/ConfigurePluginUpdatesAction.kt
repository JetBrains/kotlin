/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import org.jetbrains.kotlin.idea.actions.ConfigurePluginUpdatesDialog.EAPChannels.EAP_1_0
import org.jetbrains.kotlin.idea.actions.ConfigurePluginUpdatesDialog.EAPChannels.EAP_1_1
import javax.swing.JComponent

class ConfigurePluginUpdatesAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ConfigurePluginUpdatesDialog(project).show()
    }
}

class ConfigurePluginUpdatesDialog(project: Project) : DialogWrapper(project, false) {
    private val form = ConfigurePluginUpdatesForm()
    private val initialSelectedChannel: Int
    private var update: PluginUpdateStatus.Update? = null

    init {
        title = "Configure Kotlin Plugin Updates"
        form.updateCheckProgressIcon.suspend()
        form.updateCheckProgressIcon.setPaintPassiveIcon(false)

        form.checkForUpdatesNowButton.addActionListener {
            saveSettings()
            form.updateCheckProgressIcon.resume()
            resetUpdateStatus()
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
                close(OK_EXIT_CODE)
                KotlinPluginUpdater.getInstance().installPluginUpdate(it)
            }
        }

        form.channelCombo.addActionListener {
            resetUpdateStatus()
        }

        fun EAPChannels.indexIfAvailable() = if (hasChannel) uiIndex else null
        initialSelectedChannel = EAP_1_1.indexIfAvailable() ?:
                                 EAP_1_0.indexIfAvailable() ?: 0

        form.channelCombo.selectedIndex = initialSelectedChannel
        init()
    }

    private fun resetUpdateStatus() {
        form.updateStatusLabel.text = " "
        form.installButton.isVisible = false
    }

    override fun createCenterPanel(): JComponent = form.mainPanel

    private fun saveSettings() {
        saveSelectedChannel(form.channelCombo.selectedIndex)
    }

    private fun saveSelectedChannel(channel: Int) {
        val hosts = UpdateSettings.getInstance().storedPluginHosts
        EAPChannels.values().forEach { hosts.remove(it.url) }
        when (channel) {
            EAP_1_0.uiIndex -> hosts.add(EAP_1_0.url)
            EAP_1_1.uiIndex -> hosts.add(EAP_1_1.url)
        }
    }

    override fun doOKAction() {
        saveSettings()
        super.doOKAction()
    }

    override fun doCancelAction() {
        saveSelectedChannel(initialSelectedChannel)
        super.doCancelAction()
    }

    enum class EAPChannels(val url: String, val uiIndex: Int) {
        EAP_1_0("https://plugins.jetbrains.com/plugins/eap/6954", 1),
        EAP_1_1("https://plugins.jetbrains.com/plugins/eap-1.1/6954", 2);

        val hasChannel: Boolean get() = url in UpdateSettings.getInstance().pluginHosts
    }
}
