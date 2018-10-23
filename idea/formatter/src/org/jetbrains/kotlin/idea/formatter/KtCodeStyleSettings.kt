/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings

data class KtCodeStyleSettings(
    val custom: KotlinCodeStyleSettings,
    val common: KotlinCommonCodeStyleSettings,
    val all: CodeStyleSettings
)

fun KtCodeStyleSettings.canRestore(): Boolean {
    return custom.canRestore() || common.canRestore()
}

fun KtCodeStyleSettings.hasDefaultLoadScheme(): Boolean {
    return custom.CODE_STYLE_DEFAULTS == null || common.CODE_STYLE_DEFAULTS == null
}

fun KtCodeStyleSettings.restore() {
    custom.restore()
    common.restore()
}

fun ktCodeStyleSettings(project: Project): KtCodeStyleSettings? {
    @Suppress("DEPRECATION") // Suggested update is not supported in 173. BUNCH: 181
    val settings = CodeStyleSettingsManager.getSettings(project)

    val ktCommonSettings = settings.getCommonSettings(KotlinLanguage.INSTANCE) as KotlinCommonCodeStyleSettings
    val ktCustomSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)

    return KtCodeStyleSettings(ktCustomSettings, ktCommonSettings, settings)
}

val CodeStyleSettings.kotlinCommonSettings: KotlinCommonCodeStyleSettings
    get() = getCommonSettings(KotlinLanguage.INSTANCE) as KotlinCommonCodeStyleSettings

val CodeStyleSettings.kotlinCustomSettings: KotlinCodeStyleSettings
    get() = getCustomSettings(KotlinCodeStyleSettings::class.java)

fun CodeStyleSettings.kotlinCodeStyleDefaults(): String? {
    return kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: kotlinCommonSettings.CODE_STYLE_DEFAULTS
}