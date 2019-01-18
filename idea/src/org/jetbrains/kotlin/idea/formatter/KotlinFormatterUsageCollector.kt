/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.utils.getEnumUsage
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.KotlinFormatterUsageCollector.KotlinFormatterKind.*
import org.jetbrains.kotlin.idea.util.isDefaultOfficialCodeStyle

//todo: convert to FUS?
class KotlinFormatterUsageCollector {
    fun getProjectUsages(project: Project): Set<UsageDescriptor> {
        val usedFormatter = getKotlinFormatterKind(project)

        val settings = CodeStyleSettingsManager.getSettings(project)
        val kotlinCommonSettings = settings.kotlinCommonSettings
        val kotlinCustomSettings = settings.kotlinCustomSettings

        return setOf(
            getEnumUsage("kotlin.formatter.kind", usedFormatter),
            getEnumStringPropertyUsage(
                "kotlin.formatter.defaults",
                kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: kotlinCommonSettings.CODE_STYLE_DEFAULTS
            )
        )
    }

    private fun getEnumStringPropertyUsage(key: String, value: String?): UsageDescriptor {
        return UsageDescriptor(key + "." + value.toString().toLowerCase(java.util.Locale.ENGLISH), 1)
    }

    companion object {
        private const val GROUP_ID = "kotlin.formatter"

        private val KOTLIN_DEFAULT_COMMON = KotlinLanguageCodeStyleSettingsProvider().defaultCommonSettings
            .also { KotlinStyleGuideCodeStyle.applyToCommonSettings(it) }

        private val KOTLIN_DEFAULT_CUSTOM = KotlinCodeStyleSettings.DEFAULT.cloneSettings()
            .also { KotlinStyleGuideCodeStyle.applyToKotlinCustomSettings(it) }

        private val KOTLIN_OBSOLETE_DEFAULT_COMMON = KotlinLanguageCodeStyleSettingsProvider().defaultCommonSettings
            .also { KotlinObsoleteCodeStyle.applyToCommonSettings(it) }

        private val KOTLIN_OBSOLETE_DEFAULT_CUSTOM = KotlinCodeStyleSettings.DEFAULT.cloneSettings()
            .also { KotlinObsoleteCodeStyle.applyToKotlinCustomSettings(it) }

        fun getKotlinFormatterKind(project: Project): KotlinFormatterKind {
            val isProject = CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS
            val isDefaultOfficialCodeStyle = isDefaultOfficialCodeStyle

            val settings = CodeStyleSettingsManager.getSettings(project)
            val kotlinCommonSettings = settings.kotlinCommonSettings
            val kotlinCustomSettings = settings.kotlinCustomSettings

            val isDefaultKotlinCommonSettings = kotlinCommonSettings == KotlinLanguageCodeStyleSettingsProvider().defaultCommonSettings
            val isDefaultKotlinCustomSettings = kotlinCustomSettings == KotlinCodeStyleSettings.DEFAULT

            if (isDefaultKotlinCommonSettings && isDefaultKotlinCustomSettings) {
                return if (isDefaultOfficialCodeStyle) {
                    paired(IDEA_OFFICIAL_DEFAULT, isProject)
                } else {
                    paired(IDEA_DEFAULT, isProject)
                }
            }

            if (kotlinCommonSettings == KOTLIN_OBSOLETE_DEFAULT_COMMON && kotlinCustomSettings == KOTLIN_OBSOLETE_DEFAULT_CUSTOM) {
                return paired(IDEA_OBSOLETE_KOTLIN, isProject)
            }

            if (kotlinCommonSettings == KOTLIN_DEFAULT_COMMON && kotlinCustomSettings == KOTLIN_DEFAULT_CUSTOM) {
                return paired(IDEA_KOTLIN, isProject)
            }

            val isKotlinOfficialLikeSettings = settings == settings.clone().also {
                KotlinStyleGuideCodeStyle.apply(it)
            }
            if (isKotlinOfficialLikeSettings) {
                return paired(IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM, isProject)
            }

            val isKotlinObsoleteLikeSettings = settings == settings.clone().also {
                KotlinObsoleteCodeStyle.apply(it)
            }
            if (isKotlinObsoleteLikeSettings) {
                return paired(IDEA_KOTLIN_WITH_CUSTOM, isProject)
            }

            return paired(IDEA_CUSTOM, isProject)
        }

        private fun paired(kind: KotlinFormatterKind, isProject: Boolean): KotlinFormatterKind {
            if (!isProject) return kind

            return when (kind) {
                IDEA_DEFAULT -> PROJECT_DEFAULT
                IDEA_OFFICIAL_DEFAULT -> PROJECT_OFFICIAL_DEFAULT
                IDEA_CUSTOM -> PROJECT_CUSTOM
                IDEA_KOTLIN_WITH_CUSTOM -> PROJECT_KOTLIN_WITH_CUSTOM
                IDEA_KOTLIN -> PROJECT_KOTLIN
                IDEA_OBSOLETE_KOTLIN -> PROJECT_OBSOLETE_KOTLIN
                IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM -> PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM
                else -> kind
            }
        }
    }

    enum class KotlinFormatterKind {
        IDEA_DEFAULT,
        IDEA_CUSTOM,
        IDEA_KOTLIN_WITH_CUSTOM,
        IDEA_KOTLIN,

        PROJECT_DEFAULT,
        PROJECT_CUSTOM,
        PROJECT_KOTLIN_WITH_CUSTOM,
        PROJECT_KOTLIN,

        IDEA_OFFICIAL_DEFAULT,
        IDEA_OBSOLETE_KOTLIN,
        IDEA_OFFICIAL_KOTLIN_WITH_CUSTOM,
        PROJECT_OFFICIAL_DEFAULT,
        PROJECT_OBSOLETE_KOTLIN,
        PROJECT_OBSOLETE_KOTLIN_WITH_CUSTOM
    }
}