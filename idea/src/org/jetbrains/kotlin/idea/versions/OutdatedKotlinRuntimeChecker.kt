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

package org.jetbrains.kotlin.idea.versions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.PathUtil.getLocalFile
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.framework.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.io.IOException
import javax.swing.event.HyperlinkEvent

data class VersionedLibrary(val library: Library, val version: String?, val usedInModules: Collection<Module>)

fun findOutdatedKotlinLibraries(project: Project): List<VersionedLibrary> {
    val pluginVersion = KotlinPluginUtil.getPluginVersion()
    if ("@snapshot@" == pluginVersion) return emptyList() // plugin is run from sources, can't compare versions

    // user already clicked suppress
    if (pluginVersion == PropertiesComponent.getInstance(project).getValue(SUPPRESSED_PROPERTY_NAME)) {
        return emptyList()
    }

    val outdatedLibraries = arrayListOf<VersionedLibrary>()

    for ((library, modules) in findAllUsedLibraries(project).entrySet()) {
        val libraryVersionProperties =
                getLibraryProperties(JavaRuntimePresentationProvider.getInstance(), library) ?:
                getLibraryProperties(JSLibraryStdPresentationProvider.getInstance(), library) ?:
                continue

        val libraryVersion = libraryVersionProperties.versionString

        val runtimeVersion = bundledRuntimeVersion()

        val isOutdated = isRuntimeOutdated(libraryVersion, runtimeVersion)

        if (isOutdated) {
            outdatedLibraries.add(VersionedLibrary(library, libraryVersion, modules))
        }
    }

    return outdatedLibraries
}

fun collectModulesWithOutdatedRuntime(libraries: List<VersionedLibrary>): List<Module> =
    libraries.flatMap { it.usedInModules }

fun notifyOutdatedKotlinRuntime(project: Project, outdatedLibraries: Collection<VersionedLibrary>) {
    val pluginVersion = KotlinPluginUtil.getPluginVersion()
    val message: String = if (outdatedLibraries.size == 1) {
        val versionedLibrary = outdatedLibraries.first()

        val version = versionedLibrary.version
        val readableVersion = version ?: "unknown"
        val libraryName = versionedLibrary.library.name

        "<p>Your version of Kotlin runtime in '$libraryName' library is $readableVersion, while plugin version is $pluginVersion.</p>" +
        "<p>Runtime library should be updated to avoid compatibility problems.</p>" +
        "<p><a href=\"update\">Update Runtime</a> <a href=\"ignore\">Ignore</a></p>"
    }
    else {
        val libraryNames = outdatedLibraries.joinToString { it.library.name ?: "unknown library" }

        "<p>Version of Kotlin runtime is outdated in several libraries ($libraryNames). Plugin version is $pluginVersion.</p>" +
        "<p>Runtime libraries should be updated to avoid compatibility problems.</p>" +
        "<p><a href=\"update\">Update All</a> <a href=\"ignore\">Ignore</a></p>"
    }


    Notifications.Bus.notify(Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Outdated Kotlin Runtime", message,
                                          NotificationType.WARNING, NotificationListener { notification, event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            if ("update" == event.description) {
                val outdatedLibraries = findOutdatedKotlinLibraries(project).map { it.library }
                updateLibraries(project, outdatedLibraries)
                suggestDeleteKotlinJsIfNeeded(project, outdatedLibraries)
            }
            else if ("ignore" == event.description) {
                PropertiesComponent.getInstance(project).setValue(SUPPRESSED_PROPERTY_NAME, pluginVersion)
            }
            else {
                throw AssertionError()
            }
            notification.expire()
        }
    }), project)
}

fun deleteKotlinJs(project: Project) {
    ApplicationManager.getApplication().invokeLater {
        runWriteAction {
            val kotlinJsFile = project.baseDir.findFileByRelativePath("script/kotlin.js") ?: return@runWriteAction

            val fileToDelete = getLocalFile(kotlinJsFile)
            try {
                val parent = fileToDelete.parent
                fileToDelete.delete(null)
                parent.refresh(false, true)
            }
            catch (ex: IOException) {
                Notifications.Bus.notify(
                        Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Error", "Could not delete 'script/kotlin.js': " + ex.message, NotificationType.ERROR))
            }
        }
    }
}

private fun suggestDeleteKotlinJsIfNeeded(project: Project, outdatedLibraries: Collection<Library>) {
    project.baseDir.findFileByRelativePath("script/kotlin.js") ?: return

    var addNotification = false
    for (library in outdatedLibraries) {
        if (isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
            val jsStdlibJar = getJsStdLibJar(library)
            assert(jsStdlibJar != null) { "jslibFile should not be null" }

            if (jsStdlibJar!!.findFileByRelativePath("kotlin.js") == null) {
                addNotification = true
                break
            }
        }
    }
    if (!addNotification) return

    val message = "<p>File 'script/kotlin.js' was probably created by an older version of the Kotlin plugin.</p>" +
                  "<p>The new Kotlin plugin copies an up-to-date version of this file to the output directory automatically, so the old version of it can be deleted.</p>" +
                  "<p><a href=\"delete\">Delete this file</a> <a href=\"ignore\">Ignore</a></p>"

    Notifications.Bus.notify(Notification(OUTDATED_RUNTIME_GROUP_DISPLAY_ID, "Outdated Kotlin Runtime", message,
                                          NotificationType.WARNING, NotificationListener { notification, event ->
        if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            if ("delete" == event.description) {
                deleteKotlinJs(project)
            }
            else if ("ignore" == event.description) {
                // pass
            }
            else {
                throw AssertionError()
            }
            notification.expire()
        }
    }), project)
}

private val SUPPRESSED_PROPERTY_NAME = "oudtdated.runtime.suppressed.plugin.version"
private val OUTDATED_RUNTIME_GROUP_DISPLAY_ID = "Outdated Kotlin Runtime"

fun isRuntimeOutdated(libraryVersion: String?, runtimeVersion: String): Boolean {
    return libraryVersion == null || libraryVersion.startsWith("internal-") != runtimeVersion.startsWith("internal-") ||
           VersionComparatorUtil.compare(runtimeVersion.substringBefore("-release-"), libraryVersion) > 0
}
