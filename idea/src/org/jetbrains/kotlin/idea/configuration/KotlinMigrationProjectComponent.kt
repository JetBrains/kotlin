/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
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
import org.jetbrains.kotlin.idea.configuration.ui.showMigrationNotification
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.kotlin.idea.framework.MAVEN_SYSTEM_ID
import org.jetbrains.kotlin.idea.migration.CodeMigrationToggleAction
import org.jetbrains.kotlin.idea.migration.applicableMigrationTools
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import org.jetbrains.kotlin.idea.versions.LibInfo
import java.io.File

class KotlinMigrationProjectComponent(val project: Project) {
    @Volatile
    private var old: MigrationState? = null

    @Volatile
    private var importFinishListener: ((MigrationTestState?) -> Unit)? = null

    init {
        val connection = project.messageBus.connect()
        connection.subscribe(ProjectDataImportListener.TOPIC, ProjectDataImportListener {
            KotlinMigrationProjectComponent.getInstanceIfNotDisposed(project)?.onImportFinished()
        })
    }

    class MigrationTestState(val migrationInfo: MigrationInfo?, val hasApplicableTools: Boolean)

    @TestOnly
    fun setImportFinishListener(newListener: ((MigrationTestState?) -> Unit)?) {
        synchronized(this) {
            if (newListener != null && importFinishListener != null) {
                importFinishListener!!.invoke(null)
            }

            importFinishListener = newListener
        }
    }

    private fun notifyFinish(migrationInfo: MigrationInfo?, hasApplicableTools: Boolean) {
        importFinishListener?.invoke(MigrationTestState(migrationInfo, hasApplicableTools))
    }

    fun onImportAboutToStart() {
        if (!CodeMigrationToggleAction.isEnabled(project) || !hasChangesInProjectFiles(project)) {
            old = null
            return
        }

        old = MigrationState.build(project)
    }

    fun onImportFinished() {
        if (!CodeMigrationToggleAction.isEnabled(project) || old == null) {
            notifyFinish(null, false)
            return
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            var migrationInfo: MigrationInfo? = null
            var hasApplicableTools = false

            try {
                val new = project.runReadActionInSmartMode {
                    MigrationState.build(project)
                }

                val localOld = old.also {
                    old = null
                } ?: return@executeOnPooledThread

                migrationInfo = prepareMigrationInfo(localOld, new) ?: return@executeOnPooledThread

                if (applicableMigrationTools(migrationInfo).isEmpty()) {
                    hasApplicableTools = false
                    return@executeOnPooledThread
                } else {
                    hasApplicableTools = true
                }

                if (ApplicationManager.getApplication().isUnitTestMode) {
                    return@executeOnPooledThread
                }

                ApplicationManager.getApplication().invokeLater {
                    showMigrationNotification(project, migrationInfo)
                }
            } finally {
                notifyFinish(migrationInfo, hasApplicableTools)
            }
        }
    }

    companion object {
        fun getInstanceIfNotDisposed(project: Project): KotlinMigrationProjectComponent? {
            return runReadAction {
                if (!project.isDisposed) {
                    project.getComponent(KotlinMigrationProjectComponent::class.java)
                        ?: error("Can't find ${KotlinMigrationProjectComponent::class.qualifiedName} component")
                } else {
                    null
                }
            }
        }

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

            val checkedFiles = HashSet<File>()

            project.basePath?.let { projectBasePath ->
                checkedFiles.add(File(projectBasePath))
            }

            val changedFiles = ChangeListManager.getInstance(project).affectedPaths
            for (changedFile in changedFiles) {
                val extension = changedFile.extension
                when (extension) {
                    "gradle" -> return true
                    "properties" -> return true
                    "kts" -> return true
                    "iml" -> return true
                    "xml" -> {
                        if (changedFile.name == "pom.xml") return true
                        val parentDir = changedFile.parentFile
                        if (parentDir.isDirectory && parentDir.name == Project.DIRECTORY_STORE_FOLDER) {
                            return true
                        }
                    }
                    "kt", "java", "groovy" -> {
                        val dirs: Sequence<File> = generateSequence(changedFile) { it.parentFile }
                            .drop(1) // Drop original file
                            .takeWhile { it.isDirectory }

                        val isInBuildSrc = dirs
                            .takeWhile { checkedFiles.add(it) }
                            .any { it.name == BUILD_SRC_FOLDER_NAME }

                        if (isInBuildSrc) {
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


private const val BUILD_SRC_FOLDER_NAME = "buildSrc"
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

            val libraryInfo = parseExternalLibraryName(library) ?: continue

            if (maxStdlibInfo == null || VersionComparatorUtil.COMPARATOR.compare(libraryInfo.version, maxStdlibInfo.version) > 0) {
                maxStdlibInfo = LibInfo(KOTLIN_GROUP_ID, libraryInfo.artifactId, libraryInfo.version)
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
            if (!module.isKotlinModule()) {
                // Otherwise project compiler settings will give unreliable maximum for compiler settings
                continue
            }

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

private fun Module.isKotlinModule(): Boolean {
    if (isDisposed) return false

    if (KotlinFacet.get(this) != null) {
        return true
    }

    // This code works only for Maven and Gradle import, and it's expected that Kotlin facets are configured for
    // all modules with external system.
    return false
}