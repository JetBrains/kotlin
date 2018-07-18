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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.actions.ConfigurePluginUpdatesAction
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.psi.UserDataProperty
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

fun createOutdatedBundledCompilerMessage(project: Project, bundledCompilerVersion: String = KotlinCompilerVersion.VERSION): String? {
    val bundledCompilerMajorVersion = MajorVersion.create(bundledCompilerVersion) ?: return null

    val usedCompilerInfo = ArrayList<ModuleCompilerInfo>()
    for (module in ModuleManager.getInstance(project).modules) {
        val externalCompilerVersion = module.externalCompilerVersion ?: continue
        val externalCompilerMajorVersion = MajorVersion.create(externalCompilerVersion) ?: continue

        usedCompilerInfo.add(
            ModuleCompilerInfo(
                module,
                externalCompilerVersion,
                externalCompilerMajorVersion = externalCompilerMajorVersion,
                languageMajorVersion = MajorVersion.create(module.languageVersionSettings.languageVersion)
            )
        )
    }

    val selectedBaseModules = HashSet<Module>()
    val selectedNewerModulesInfos = ArrayList<ModuleCompilerInfo>()
    val moduleSourceRootMap = ModuleSourceRootMap(project)
    for (moduleCompilerInfo in usedCompilerInfo) {
        val languageMajorVersion = moduleCompilerInfo.languageMajorVersion
        val externalCompilerMajorVersion = moduleCompilerInfo.externalCompilerMajorVersion

        if (externalCompilerMajorVersion > bundledCompilerMajorVersion && languageMajorVersion > bundledCompilerMajorVersion) {
            val wholeModuleGroup = moduleSourceRootMap.getWholeModuleGroup(moduleCompilerInfo.module)
            if (!selectedBaseModules.contains(wholeModuleGroup.baseModule)) {
                // Remap to base module
                selectedNewerModulesInfos.add(
                    ModuleCompilerInfo(
                        wholeModuleGroup.baseModule,
                        moduleCompilerInfo.externalCompilerVersion,
                        externalCompilerMajorVersion = externalCompilerMajorVersion,
                        languageMajorVersion = languageMajorVersion
                    )
                )
                selectedBaseModules.add(wholeModuleGroup.baseModule)
            }
        }

        if (selectedNewerModulesInfos.size > NUMBER_OF_MODULES_TO_SHOW) {
            break
        }
    }

    if (selectedNewerModulesInfos.isEmpty()) {
        return null
    }

    var modulesStr =
        selectedNewerModulesInfos.take(NUMBER_OF_MODULES_TO_SHOW).joinToString(separator = "") {
            "<li>${it.module.name} (${it.externalCompilerVersion})</li><br/>"
        }

    if (selectedNewerModulesInfos.size > NUMBER_OF_MODULES_TO_SHOW) {
        modulesStr += "<li> ... </li>"
    }

    // <br/> are going to be replaced to \n for the `Event Log` view. Remove last <br/>, to avoid additional empty line.
    modulesStr = modulesStr.removeSuffix("<br/>")

    return """
        <p>The compiler bundled to Kotlin plugin ($bundledCompilerVersion) is older than external compiler used for building modules:</p>
        <ul>$modulesStr</ul>
        <p>This may cause different set of errors and warnings reported in IDE.</p>
        <p><a href="update">Update</a>  <a href="ignore">Ignore</a></p>"""
        .trimIndent().lines().joinToString(separator = "").replace("<br/>", "\n")
}

private class ModuleCompilerInfo(
    val module: Module,
    val externalCompilerVersion: String,
    val externalCompilerMajorVersion: MajorVersion,
    val languageMajorVersion: MajorVersion)

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

private const val NUMBER_OF_MODULES_TO_SHOW = 2
private const val SUPPRESSED_OUTDATED_COMPILER_PROPERTY_NAME = "oudtdated.bundled.kotlin.compiler"
private const val OUTDATED_BUNDLED_COMPILER_GROUP_DISPLAY_ID = "Outdated Bundled Kotlin Compiler"