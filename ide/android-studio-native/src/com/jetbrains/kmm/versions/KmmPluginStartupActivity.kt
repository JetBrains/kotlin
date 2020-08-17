/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.versions

import com.intellij.ide.plugins.PluginManagerConfigurable
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.jetbrains.kmm.KMM_LOG
import com.jetbrains.kmm.KmmBundle
import com.jetbrains.kmm.UserNotification
import com.jetbrains.kmm.versions.KmmCompatibilityChecker.CompatibilityCheckResult.*
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.KotlinPluginVersion

class KmmPluginStartupActivity : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        KmmCompatibilityChecker.checkCompatibilityAgainstBigKotlin(project)
    }
}

private val VERSIONS_INFO = listOf(
    KmmBundle.message("version.kotlinPlugin", KotlinPluginUtil.getPluginVersion()),
    KmmBundle.message("version.compiledAgainstKotlin", MobileMultiplatformPluginVersionsInfo.compiledAgainstKotlin),
    KmmBundle.message("version.kmmPlugin", MobileMultiplatformPluginVersionsInfo.pluginVersion)
)

object KmmCompatibilityChecker {
    fun checkCompatibilityAgainstBigKotlin(project: Project) {
        KMM_LOG.debug("KmmCompatibilityChecker:")
        VERSIONS_INFO.forEach { KMM_LOG.debug(it) }

        if (MobileMultiplatformPluginVersionsInfo.isDevelopment()) return

        val actualKotlinPluginVersion = KotlinPluginVersion.parse(KotlinPluginUtil.getPluginVersion()) ?: return
        val compiledAgainstKotlinVersion = KotlinPluginVersion.parse(MobileMultiplatformPluginVersionsInfo.compiledAgainstKotlin) ?: return

        val errorText = when (checkVersions(actualKotlinPluginVersion, compiledAgainstKotlinVersion)) {
            COMPATIBLE -> null
            OUTDATED_KOTLIN -> KmmBundle.message("startup.error.outdatedKotlin")
            OUTDATED_KMM_PLUGIN -> KmmBundle.message("startup.error.outdatedPlugin")
            UNKNOWN -> KmmBundle.message("startup.error.unknown") + VERSIONS_INFO.joinToString("<br/>")
        }

        if (errorText != null) {
            ApplicationManager.getApplication().invokeLater {
                UserNotification(project).showError(
                    KmmBundle.message("startup.error.title"),
                    errorText,
                    object : NotificationAction(KmmBundle.message("startup.error.suggestion.action")) {
                        override fun actionPerformed(action: AnActionEvent, notification: Notification) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(null, PluginManagerConfigurable::class.java)
                            notification.hideBalloon()
                        }
                    }
                )

                disablePlugin("com.jetbrains.kmm")
            }
        }
    }

    fun checkVersions(actualKotlinPlugin: KotlinPluginVersion, compiledAgainstKotlinPlugin: KotlinPluginVersion): CompatibilityCheckResult {
        val actualKotlinVersion =
            KotlinVersion.parseFromString(actualKotlinPlugin.kotlinVersion)?.stripHotfixes() ?: return UNKNOWN
        val compiledAgainstKotlinVersion =
            KotlinVersion.parseFromString(compiledAgainstKotlinPlugin.kotlinVersion)
                ?.stripHotfixes() ?: return UNKNOWN


        return when {
            actualKotlinVersion < compiledAgainstKotlinVersion -> OUTDATED_KOTLIN
            actualKotlinVersion > compiledAgainstKotlinVersion.nextPatchVersion() -> OUTDATED_KMM_PLUGIN
            else -> COMPATIBLE
        }
    }

    enum class CompatibilityCheckResult {
        COMPATIBLE, OUTDATED_KOTLIN, OUTDATED_KMM_PLUGIN, UNKNOWN
    }
}

internal data class KotlinVersion(val major: Int, val minor: Int, val patch: Int, val isSnapshot: Boolean) : Comparable<KotlinVersion> {

    override fun compareTo(other: KotlinVersion): Int = when {
        major != other.major -> major - other.major
        minor != other.minor -> minor - other.minor
        patch != other.patch -> patch - other.patch
        else -> 0
    }

    fun stripHotfixes(): KotlinVersion {
        require(!isSnapshot) { "Can't produce next patch for SNAPSHOT-versions" }
        // 1.2.52 -> 52 -> 50
        val patchWithoutHotfixes = (patch / 10) * 10

        return KotlinVersion(major, minor, patchWithoutHotfixes, false)
    }

    fun nextPatchVersion(): KotlinVersion {
        require(!isSnapshot) { "Can't produce next patch for SNAPSHOT-versions" }

        // 1.2.52 -> 52 -> 50
        val patchWithoutHotfixes = (patch / 10) * 10

        val nextPatchWithoutHotfixes = patchWithoutHotfixes + 10

        return KotlinVersion(major, minor, nextPatchWithoutHotfixes, false)
    }

    companion object {
        private const val SNAPSHOT_SUFFIX = "-SNAPSHOT"

        fun parseFromString(versionString: String): KotlinVersion? {
            val isSnapshot = versionString.endsWith(SNAPSHOT_SUFFIX)
            val versionWithoutSnapshotSuffix = versionString.removeSuffix(SNAPSHOT_SUFFIX)

            val segments = versionWithoutSnapshotSuffix.split(".")
            val major = segments.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = segments.getOrNull(1)?.toIntOrNull() ?: return null
            val patch = segments.getOrNull(2)?.toIntOrNull() ?: 0 // SNAPSHOT versions don't have patch-version

            return KotlinVersion(major, minor, patch, isSnapshot)
        }
    }
}