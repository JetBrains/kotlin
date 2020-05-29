/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.roots

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.utils.addToStdlib.lastIndexOfOrNull

/**
 * Internal logic about finding script root for [GradleBuildRootsManager].
 * Extracted only for tests.
 *
 * @see GradleBuildRootsManager for details.
 */
abstract class GradleBuildRootsLocator {
    protected val roots = GradleBuildRootIndex()

    abstract fun getScriptInfo(localPath: String): GradleScriptInfo?

    fun getBuildRootByWorkingDir(gradleWorkingDir: String) =
        roots.getBuildByRootDir(gradleWorkingDir)

    fun getScriptInfo(file: VirtualFile): GradleScriptInfo? =
        getScriptInfo(file.localPath)

    private val VirtualFile.localPath
        get() = path

    fun maybeAffectedGradleProjectFile(filePath: String): Boolean =
        filePath.endsWith("/gradle.properties") ||
                filePath.endsWith("/gradle.local") ||
                filePath.endsWith("/gradle-wrapper.properties") ||
                filePath.endsWith(".gradle.kts")

    fun isAffectedGradleProjectFile(filePath: String): Boolean =
        findAffectedFileRoot(filePath) != null ||
                roots.isStandaloneScript(filePath)

    fun findAffectedFileRoot(filePath: String): GradleBuildRoot? {
        if (filePath.endsWith("/gradle.properties") ||
            filePath.endsWith("/gradle.local")
        ) {
            return roots.getBuildByProjectDir(filePath.substringBeforeLast("/"))
        } else if (filePath.endsWith("/gradle-wrapper.properties")) {
            val gradleWrapperDirIndex = filePath.lastIndexOfOrNull('/') ?: return null
            val gradleDirIndex = filePath.lastIndexOfOrNull('/', gradleWrapperDirIndex - 1) ?: return null
            val buildDirIndex = filePath.lastIndexOfOrNull('/', gradleDirIndex - 1) ?: return null
            return roots.getBuildByRootDir(filePath.substring(0, buildDirIndex))
        }

        return findScriptBuildRoot(filePath, searchNearestLegacy = false)?.root as? GradleBuildRoot
    }

    enum class NotificationKind {
        dontCare, // one of: imported, inside linked legacy gradle build
        outsideAnyting, // suggest link related gradle build or just say that there is no one
        wasNotImportedAfterCreation, // project not yet imported after this file was created
        notEvaluatedInLastImport, // all other scripts, suggest to sync or mark as standalone
        standalone
    }

    /**
     * Timestamp of an moment when script file was discovered (indexed).
     * Used to detect if that script was existed at the moment of import
     */
    abstract fun getScriptFirstSeenTs(path: String): Long

    inner class ScriptUnderRoot(
        val filePath: String,
        val root: GradleBuildRoot?,
        val script: GradleScriptInfo? = null,
        val standalone: Boolean = false,
        val nearest: GradleBuildRoot? = root
    ) {
        val notificationKind: NotificationKind
            get() = when {
                isImported -> NotificationKind.dontCare
                standalone -> NotificationKind.standalone
                nearest == null -> NotificationKind.outsideAnyting
                nearest.importing -> NotificationKind.dontCare
                else -> when (nearest) {
                    is Legacy -> NotificationKind.dontCare
                    is New -> NotificationKind.wasNotImportedAfterCreation
                    is Imported -> when {
                        wasImportedAndNotEvaluated -> NotificationKind.notEvaluatedInLastImport
                        else -> NotificationKind.wasNotImportedAfterCreation
                    }
                }
            }

        private val isImported: Boolean
            get() = script != null

        private val wasImportedAndNotEvaluated: Boolean
            get() = nearest is Imported &&
                    getScriptFirstSeenTs(filePath) < nearest.data.importTs

        override fun toString(): String {
            return "ScriptUnderRoot(root=$root, script=$script, standalone=$standalone, nearest=$nearest)"
        }
    }

    fun findScriptBuildRoot(gradleKtsFile: VirtualFile): ScriptUnderRoot? =
        findScriptBuildRoot(gradleKtsFile.path)

    fun findScriptBuildRoot(filePath: String, searchNearestLegacy: Boolean = true): ScriptUnderRoot? {
        if (!filePath.endsWith(".gradle.kts")) return null

        val scriptInfo = getScriptInfo(filePath)
        val imported = scriptInfo?.buildRoot
        if (imported != null) return ScriptUnderRoot(filePath, imported, scriptInfo)

        if (filePath.endsWith("/build.gradle.kts") ||
            filePath.endsWith("/settings.gradle.kts") ||
            filePath.endsWith("/init.gradle.kts")
        ) {
            // build|settings|init.gradle.kts scripts should be located near gradle project root only
            val gradleBuild = roots.getBuildByProjectDir(filePath.substringBeforeLast("/"))
            if (gradleBuild != null) return ScriptUnderRoot(filePath, gradleBuild)
        }

        // other scripts: "included", "precompiled" scripts, scripts in unlinked projects,
        // or just random files with ".gradle.kts" ending

        val standaloneScriptRoot = roots.getStandaloneScriptRoot(filePath)
        if (standaloneScriptRoot != null) {
            return ScriptUnderRoot(filePath, standaloneScriptRoot, standalone = true)
        }

        if (searchNearestLegacy) {
            val nearest = roots.findNearestRoot(filePath)
            return when (nearest) {
                is Legacy -> ScriptUnderRoot(filePath, nearest)
                else -> ScriptUnderRoot(filePath, null, nearest = nearest)
            }
        } else {
            return ScriptUnderRoot(filePath, null)
        }
    }
}