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

package org.jetbrains.kotlin.idea.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import javax.swing.JComponent

class ConfigurePluginUpdatesAction : AnAction() {
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
                        form.updateStatusLabel.text = "You have the latest version of the plugin installed."

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

        initialSelectedChannel = if (hasEAPChannel()) 1 else 0
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
        when (channel) {
            0 -> hosts.remove(EAP_UPDATE_HOST)
            1 -> if (EAP_UPDATE_HOST !in hosts) hosts.add(EAP_UPDATE_HOST)
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

    private fun hasEAPChannel() = EAP_UPDATE_HOST in UpdateSettings.getInstance().pluginHosts

    companion object {
        val EAP_UPDATE_HOST = "https://plugins.jetbrains.com/plugins/eap/6954"
    }
}
