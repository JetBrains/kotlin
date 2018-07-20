/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.DataManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.actions.ConfigurePluginUpdatesAction
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.UserDataProperty
import java.util.concurrent.ConcurrentHashMap
import javax.swing.event.HyperlinkEvent

var Module.externalCompilerVersion: String? by UserDataProperty(Key.create("EXTERNAL_COMPILER_VERSION"))

fun notifyOutdatedBundledCompilerIfNecessary(project: Project) {
    val pluginVersion = KotlinPluginUtil.getPluginVersion()
    if (pluginVersion == PropertiesComponent.getInstance(project).getValue(SUPPRESSED_OUTDATED_COMPILER_PROPERTY_NAME)) {
        return
    }

    val message: String = createOutdatedBundledCompilerMessage(project) ?: return

    if (ApplicationManager.getApplication().isUnitTestMode) {
        return
    }

    Notifications.Bus.notify(
        Notification(
            OUTDATED_BUNDLED_COMPILER_GROUP_DISPLAY_ID, OUTDATED_BUNDLED_COMPILER_GROUP_DISPLAY_ID, message,
            NotificationType.WARNING, NotificationListener { notification, event ->
                if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                    when {
                        "update" == event.description -> {
                            val action = ActionManager.getInstance().getAction(ConfigurePluginUpdatesAction.ACTION_ID)
                            val dataContext = DataManager.getInstance().dataContextFromFocus.result
                            val actionEvent = AnActionEvent.createFromAnAction(action, null, ActionPlaces.ACTION_SEARCH, dataContext)
                            action.actionPerformed(actionEvent)
                        }
                        "ignore" == event.description -> {
                            if (!project.isDisposed) {
                                PropertiesComponent.getInstance(project).setValue(SUPPRESSED_OUTDATED_COMPILER_PROPERTY_NAME, pluginVersion)
                            }
                        }
                        else -> {
                            throw AssertionError()
                        }
                    }
                    notification.expire()
                }
            }
        ),
        project
    )
}

private var alreadyNotified = ConcurrentHashMap<String, String>()

fun createOutdatedBundledCompilerMessage(project: Project, bundledCompilerVersion: String = KotlinCompilerVersion.VERSION): String? {
    val bundledCompilerMajorVersion = MajorVersion.create(bundledCompilerVersion) ?: return null

    var maxCompilerVersion: String? = null
    for (module in ModuleManager.getInstance(project).modules) {
        val externalCompilerVersion = module.externalCompilerVersion ?: continue
        val externalCompilerMajorVersion = MajorVersion.create(externalCompilerVersion) ?: continue
        val languageMajorVersion = MajorVersion.create(module.languageVersionSettings.languageVersion)

        if (externalCompilerMajorVersion > bundledCompilerMajorVersion && languageMajorVersion > bundledCompilerMajorVersion) {
            if (maxCompilerVersion == null ||
                VersionComparatorUtil.COMPARATOR.compare(externalCompilerVersion, maxCompilerVersion) > 0
            ) {
                maxCompilerVersion = externalCompilerVersion
            }
        }
    }

    if (maxCompilerVersion == null) {
        return null
    }

    val lastProjectNotified = alreadyNotified[project.name]
    if (lastProjectNotified == maxCompilerVersion) {
        if (!ApplicationManager.getApplication().isUnitTestMode) {
            return null
        }
    }
    alreadyNotified[project.name] = maxCompilerVersion

    return """
        <p>The compiler bundled to Kotlin plugin ($bundledCompilerVersion) is older than external compiler ($maxCompilerVersion).</p>
        <p>Kotlin plugin should be updated to avoid compatibility problems.</p>
        <p><a href="update">Update</a>  <a href="ignore">Ignore</a></p>"""
        .trimIndent().lines().joinToString(separator = "").replace("<br/>", "\n")
}

private data class MajorVersion(val major: Int, val minor: Int) : Comparable<MajorVersion> {
    override fun compareTo(other: MajorVersion): Int {
        if (major > other.major) return 1
        if (major < other.major) return -1

        if (minor > other.minor) return 1
        if (minor < other.minor) return -1

        return 0
    }

    override fun toString(): String = "$major.$minor"

    companion object {
        fun create(languageVersion: LanguageVersion): MajorVersion {
            return MajorVersion(languageVersion.major, languageVersion.minor)
        }

        fun create(versionStr: String): MajorVersion? {
            if (versionStr == "@snapshot@") {
                return MajorVersion(Int.MAX_VALUE, Int.MAX_VALUE)
            }

            val regex = "(\\d+)\\.(\\d+).*".toRegex()

            val matchResult = regex.matchEntire(versionStr) ?: return null

            val major: Int = matchResult.groupValues[1].toInt()
            val minor: Int = matchResult.groupValues[2].toInt()

            return MajorVersion(major, minor)
        }
    }
}

private const val SUPPRESSED_OUTDATED_COMPILER_PROPERTY_NAME = "outdated.bundled.kotlin.compiler"
private const val OUTDATED_BUNDLED_COMPILER_GROUP_DISPLAY_ID = "Outdated Bundled Kotlin Compiler"