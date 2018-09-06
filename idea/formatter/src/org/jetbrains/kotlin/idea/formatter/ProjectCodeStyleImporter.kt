/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.codeStyle.CodeStyleSchemesModel
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSchemes
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemesImpl

object ProjectCodeStyleImporter {
    fun apply(project: Project, codeStyle: KotlinPredefinedCodeStyle) {
        val schemeManager = CodeStyleSettingsManager.getInstance(project)
        val schemesModel = CodeStyleSchemesModel(project)

        val projectScheme = schemesModel.projectScheme

        val currentScheme =
            if (schemeManager.USE_PER_PROJECT_SETTINGS)
                projectScheme
            else
                CodeStyleSchemes.getInstance().findPreferredScheme(schemeManager.PREFERRED_PROJECT_CODE_STYLE)

        if (projectScheme != currentScheme) {
            schemeManager.USE_PER_PROJECT_SETTINGS = true
            schemeManager.PREFERRED_PROJECT_CODE_STYLE = null

            CodeStyleSchemesImpl.getSchemeManager().setSchemes(listOf(), null, null)
        }

        val codeStyleSettings = projectScheme.codeStyleSettings

        val kotlinCommonSettings = codeStyleSettings.kotlinCommonSettings
        val kotlinCustomSettings = codeStyleSettings.kotlinCustomSettings

        val defaults = kotlinCustomSettings.CODE_STYLE_DEFAULTS ?: kotlinCommonSettings.CODE_STYLE_DEFAULTS
        if (defaults != codeStyle.codeStyleId) {
            codeStyle.apply(codeStyleSettings)
            schemeManager.mainProjectCodeStyle = codeStyleSettings
        }
    }
}