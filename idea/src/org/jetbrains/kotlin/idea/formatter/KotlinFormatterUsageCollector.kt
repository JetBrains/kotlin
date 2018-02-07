/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.internal.statistic.beans.GroupDescriptor
import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.util.compat.statistic.AbstractProjectsUsagesCollector
import org.jetbrains.kotlin.idea.util.compat.statistic.getEnumUsage

class KotlinFormatterUsageCollector : AbstractProjectsUsagesCollector() {
    override fun getGroupId(): GroupDescriptor = GroupDescriptor.create(GROUP_ID)

    override fun getProjectUsages(project: Project): Set<UsageDescriptor> {
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

        private val KOTLIN_DEFAULT_CUSTOM = KotlinCodeStyleSettings.DEFAULT.clone()
            .also { KotlinStyleGuideCodeStyle.applyToKotlinCustomSettings(it as KotlinCodeStyleSettings) }

        fun getKotlinFormatterKind(project: Project): KotlinFormatterKind {
            val isProject = CodeStyleSettingsManager.getInstance(project).USE_PER_PROJECT_SETTINGS

            val settings = CodeStyleSettingsManager.getSettings(project)
            val kotlinCommonSettings = settings.kotlinCommonSettings
            val kotlinCustomSettings = settings.kotlinCustomSettings

            val isDefaultKotlinCommonSettings = kotlinCommonSettings == KotlinLanguageCodeStyleSettingsProvider().defaultCommonSettings
            val isDefaultKotlinCustomSettings = kotlinCustomSettings == KotlinCodeStyleSettings.DEFAULT
            if (isDefaultKotlinCommonSettings && isDefaultKotlinCustomSettings) {
                return if (isProject) KotlinFormatterKind.PROJECT_DEFAULT else KotlinFormatterKind.IDEA_DEFAULT
            }

            val isOnlyKotlinStyle = kotlinCommonSettings == KOTLIN_DEFAULT_COMMON && kotlinCustomSettings == KOTLIN_DEFAULT_CUSTOM
            if (isOnlyKotlinStyle) {
                return if (isProject) KotlinFormatterKind.PROJECT_KOTLIN else KotlinFormatterKind.IDEA_KOTLIN
            }

            val isKotlinLikeSettings = settings == settings.clone().also {
                KotlinStyleGuideCodeStyle.apply(it)
            }
            if (isKotlinLikeSettings) {
                return if (isProject) KotlinFormatterKind.PROJECT_KOTLIN_WITH_CUSTOM else KotlinFormatterKind.IDEA_KOTLIN_WITH_CUSTOM
            }

            return if (isProject) KotlinFormatterKind.PROJECT_CUSTOM else KotlinFormatterKind.IDEA_CUSTOM
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
        PROJECT_KOTLIN;
    }
}