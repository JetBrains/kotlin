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

package org.jetbrains.kotlin.idea.reporter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.ui.Messages
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import java.awt.Component
import javax.swing.Icon

/**
 * We need to wrap ITNReporter for force showing or errors from kotlin plugin even from released version of IDEA.
 */
class KotlinReportSubmitter : ITNReporterCompat() {
    private var hasUpdate = false
    private var hasLatestVersion = false

    override fun showErrorInRelease(event: IdeaLoggingEvent): Boolean {
        val notificationEnabled = "disabled" != System.getProperty("kotlin.fatal.error.notification", "enabled")
        return notificationEnabled && (!hasUpdate || ApplicationManager.getApplication().isInternal)
    }

    override fun submitCompat(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component?,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        if (hasUpdate) {
            if (ApplicationManager.getApplication().isInternal) {
                return super.submitCompat(events, additionalInfo, parentComponent, consumer)
            }
            return true
        }

        if (hasLatestVersion) {
            return super.submitCompat(events, additionalInfo, parentComponent, consumer)
        }

        KotlinPluginUpdater.getInstance().runUpdateCheck { status ->
            if (status is PluginUpdateStatus.Update) {
                hasUpdate = true

                if (ApplicationManager.getApplication().isInternal) {
                    super.submitCompat(events, additionalInfo, parentComponent, consumer)
                }

                val rc = showDialog(
                    parentComponent,
                    "You're running Kotlin plugin version ${KotlinPluginUtil.getPluginVersion()}, " +
                            "while the latest version is ${status.pluginDescriptor.version}",
                    "Update Kotlin Plugin",
                    arrayOf("Update", "Ignore"),
                    0, Messages.getInformationIcon()
                )

                if (rc == 0) {
                    KotlinPluginUpdater.getInstance().installPluginUpdate(status)
                }
            } else {
                hasLatestVersion = true
                super.submitCompat(events, additionalInfo, parentComponent, consumer)
            }
            false
        }
        return true
    }

    fun showDialog(parent: Component?, message: String, title: String, options: Array<String>, defaultOptionIndex: Int, icon: Icon?): Int {
        return if (parent != null) {
            Messages.showDialog(parent, message, title, options, defaultOptionIndex, icon)
        } else {
            Messages.showDialog(message, title, options, defaultOptionIndex, icon)
        }
    }
}
