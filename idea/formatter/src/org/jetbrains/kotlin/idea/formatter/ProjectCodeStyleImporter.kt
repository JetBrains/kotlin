/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager

object ProjectCodeStyleImporter {
    fun apply(project: Project, codeStyleStr: String?): Boolean {
        return when (codeStyleStr) {
            KotlinObsoleteCodeStyle.CODE_STYLE_SETTING -> {
                apply(project, KotlinObsoleteCodeStyle.INSTANCE)
                true
            }
            KotlinStyleGuideCodeStyle.CODE_STYLE_SETTING -> {
                apply(project, KotlinStyleGuideCodeStyle.INSTANCE)
                true
            }
            else -> false
        }
    }

    fun apply(project: Project, predefinedCodeStyle: KotlinPredefinedCodeStyle) {
        val settingsManager = CodeStyleSettingsManager.getInstance(project)

        val customSettings = CodeStyle.getSettings(project)
        if (predefinedCodeStyle.codeStyleId == customSettings.kotlinCodeStyleDefaults()) {
            // Don't bother user that already have correct code style
            return
        }

        val projectSettingsUpdated: CodeStyleSettings = if (settingsManager.USE_PER_PROJECT_SETTINGS) {
            customSettings.clone()
        } else {
            CodeStyleSettings()
        }

        settingsManager.USE_PER_PROJECT_SETTINGS = true

        predefinedCodeStyle.apply(projectSettingsUpdated)
        settingsManager.mainProjectCodeStyle = projectSettingsUpdated
    }
}