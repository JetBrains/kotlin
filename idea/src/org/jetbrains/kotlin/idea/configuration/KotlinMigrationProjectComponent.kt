/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx
import com.intellij.util.CommonProcessors
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.configuration.ui.MigrationNotificationDialog
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.framework.MAVEN_SYSTEM_ID
import org.jetbrains.kotlin.idea.migration.CodeMigrationAction
import org.jetbrains.kotlin.idea.migration.CodeMigrationToggleAction
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.versions.LibInfo

class KotlinMigrationProjectComponent(val project: Project) {
    private var old: MigrationState? = null
    private var new: MigrationState? = null

    private var lastMigrationInfo: MigrationInfo? = null

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            KotlinMigrationProjectComponent.getInstance(project).onImportFinished()
        })
    }

    @Synchronized
    @TestOnly
    fun requestLastMigrationInfo(): MigrationInfo? {
        val temp = lastMigrationInfo
        lastMigrationInfo = null
        return temp
    }

    @Synchronized
    fun onImportAboutToStart() {
        if (!CodeMigrationToggleAction.isEnabled(project) || !hasChangesInProjectFiles(project)) {
            old = null
            return
        }

        lastMigrationInfo = null

        old = MigrationState.build(project)
    }

    @Synchronized
    fun onImportFinished() {
        if (!CodeMigrationToggleAction.isEnabled(project)) {
            return
        }

        if (old == null) return;

        new = MigrationState.build(project)

        val migrationInfo = prepareMigrationInfo(old, new) ?: return

        old = null
        new = null

        if (ApplicationManager.getApplication().isUnitTestMode) {
            lastMigrationInfo = migrationInfo
            return
        }

        ApplicationManager.getApplication().invokeLater {
            val migrationNotificationDialog = MigrationNotificationDialog(project, migrationInfo)
            migrationNotificationDialog.show()

            if (migrationNotificationDialog.isOK) {
                val action = ActionManager.getInstance().getAction(CodeMigrationAction.ACTION_ID)

                val dataContext = getDataContextFromDialog(migrationNotificationDialog)
                if (dataContext != null) {
                    val actionEvent = AnActionEvent.createFromAnAction(action, null, ActionPlaces.ACTION_SEARCH, dataContext)

                    action.actionPerformed(actionEvent)
                }
            }
        }
    }

    companion object {
        fun getInstance(project: Project): KotlinMigrationProjectComponent =
            project.getComponent(KotlinMigrationProjectComponent::class.java)!!

        private fun prepareMigrationInfo(old: MigrationState?, new: MigrationState?): MigrationInfo? {
            if (old == null || new == null) {
                return null
            }

            val oldLibraryVersion = old.stdlibInfo?.version
            val newLibraryVersion = new.stdlibInfo?.version

            if (oldLibraryVersion == null || newLibraryVersion == null) {
                return null
            }

            if (VersionComparatorUtil.COMPARATOR.compare(newLibraryVersion, oldLibraryVersion) > 0 ||
                old.apiVersion < new.apiVersion || old.languageVersion < new.languageVersion
            ) {
                return MigrationInfo(
                    oldLibraryVersion, newLibraryVersion,
                    old.apiVersion, new.apiVersion,
                    old.languageVersion, new.languageVersion
                )
            }

            return null
        }

        private fun hasChangesInProjectFiles(project: Project): Boolean {
            if (ProjectLevelVcsManagerEx.getInstance(project).allVcsRoots.isEmpty()) {
                return true
            }

            val changedFiles = ChangeListManager.getInstance(project).affectedPaths
            for (changedFile in changedFiles) {
                val extension = changedFile.extension
                when (extension) {
                    "gradle" -> return true
                    "properties" -> return true
                    "iml" -> return true
                    "xml" -> {
                        if (changedFile.name == "pom.xml") return true
                        val parentDir = changedFile.parentFile
                        if (parentDir.isDirectory && parentDir.name == Project.DIRECTORY_STORE_FOLDER) {
                            return true
                        }
                    }
                }
            }

            return false
        }
    }
}

private class MigrationState(
    var stdlibInfo: LibInfo?,
    var apiVersion: ApiVersion,
    var languageVersion: LanguageVersion
) {
    companion object {
        fun build(project: Project): MigrationState {
            val libraries = maxKotlinLibVersion(project)
            val languageVersionSettings = collectMaxCompilerSettings(project)
            return MigrationState(libraries, languageVersionSettings.apiVersion, languageVersionSettings.languageVersion)
        }
    }
}

data class MigrationInfo(
    val oldStdlibVersion: String,
    val newStdlibVersion: String,
    val oldApiVersion: ApiVersion,
    val newApiVersion: ApiVersion,
    val oldLanguageVersion: LanguageVersion,
    val newLanguageVersion: LanguageVersion
) {
    companion object {
        fun create(
            oldStdlibVersion: String,
            oldApiVersion: ApiVersion,
            oldLanguageVersion: LanguageVersion,
            newStdlibVersion: String = oldStdlibVersion,
            newApiVersion: ApiVersion = oldApiVersion,
            newLanguageVersion: LanguageVersion = oldLanguageVersion
        ): MigrationInfo {
            return MigrationInfo(
                oldStdlibVersion, newStdlibVersion,
                oldApiVersion, newApiVersion,
                oldLanguageVersion, newLanguageVersion
            )
        }
    }
}

fun MigrationInfo.isLanguageVersionUpdate(old: LanguageVersion, new: LanguageVersion): Boolean {
    return oldLanguageVersion <= old && newLanguageVersion >= new
}

private const val KOTLIN_GROUP_ID = "org.jetbrains.kotlin"

private fun maxKotlinLibVersion(project: Project): LibInfo? {
    return runReadAction {
        var maxStdlibInfo: LibInfo? = null

        val allLibProcessor = CommonProcessors.CollectUniquesProcessor<Library>()
        ProjectRootManager.getInstance(project).orderEntries().forEachLibrary(allLibProcessor)

        for (library in allLibProcessor.results) {
            if (!ExternalSystemApiUtil.isExternalSystemLibrary(library, GRADLE_SYSTEM_ID) &&
                !ExternalSystemApiUtil.isExternalSystemLibrary(library, MAVEN_SYSTEM_ID)
            ) {
                continue
            }

            if (library.name?.contains(" $KOTLIN_GROUP_ID:kotlin-stdlib") != true) {
                continue
            }

            val libName = library.name ?: continue

            val version = libName.substringAfterLastNullable(":") ?: continue
            val artifactId = libName.substringBeforeLastNullable(":")?.substringAfterLastNullable(":") ?: continue

            if (version.isBlank() || artifactId.isBlank()) continue

            if (maxStdlibInfo == null || VersionComparatorUtil.COMPARATOR.compare(version, maxStdlibInfo.version) > 0) {
                maxStdlibInfo = LibInfo(KOTLIN_GROUP_ID, artifactId, version)
            }
        }

        maxStdlibInfo
    }
}

private fun collectMaxCompilerSettings(project: Project): LanguageVersionSettings {
    return runReadAction {
        var maxApiVersion: ApiVersion? = null
        var maxLanguageVersion: LanguageVersion? = null

        for (module in ModuleManager.getInstance(project).modules) {
            val languageVersionSettings = module.languageVersionSettings

            if (maxApiVersion == null || languageVersionSettings.apiVersion > maxApiVersion) {
                maxApiVersion = languageVersionSettings.apiVersion
            }

            if (maxLanguageVersion == null || languageVersionSettings.languageVersion > maxLanguageVersion) {
                maxLanguageVersion = languageVersionSettings.languageVersion
            }
        }

        LanguageVersionSettingsImpl(maxLanguageVersion ?: LanguageVersion.LATEST_STABLE, maxApiVersion ?: ApiVersion.LATEST_STABLE)
    }
}

fun String.substringBeforeLastNullable(delimiter: String, missingDelimiterValue: String? = null): String? {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

fun String.substringAfterLastNullable(delimiter: String, missingDelimiterValue: String? = null): String? {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}