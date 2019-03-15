/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings

class KotlinObsoleteCodeStyle : KotlinPredefinedCodeStyle(CODE_STYLE_TITLE, KotlinLanguage.INSTANCE) {
    override val codeStyleId: String = CODE_STYLE_ID

    override fun apply(settings: CodeStyleSettings) {
        Companion.apply(settings)
    }

    companion object {
        val INSTANCE = KotlinObsoleteCodeStyle()

        const val CODE_STYLE_ID = "KOTLIN_OLD_DEFAULTS"
        const val CODE_STYLE_SETTING = "obsolete"
        const val CODE_STYLE_TITLE = "Kotlin obsolete IntelliJ IDEA codestyle"

        fun apply(settings: CodeStyleSettings) {
            applyToKotlinCustomSettings(settings.kotlinCustomSettings)
            applyToCommonSettings(settings.kotlinCommonSettings)
        }

        fun applyToKotlinCustomSettings(kotlinCustomSettings: KotlinCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            kotlinCustomSettings.apply {
                if (modifyCodeStyle) {
                    CODE_STYLE_DEFAULTS = CODE_STYLE_ID
                }

                CONTINUATION_INDENT_IN_PARAMETER_LISTS = true
                CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true
                CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true
                CONTINUATION_INDENT_FOR_CHAINED_CALLS = true
                CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true
                CONTINUATION_INDENT_IN_IF_CONDITIONS = true
                CONTINUATION_INDENT_IN_ELVIS = true
                WRAP_EXPRESSION_BODY_FUNCTIONS = CodeStyleSettings.DO_NOT_WRAP
                IF_RPAREN_ON_NEW_LINE = false
            }
        }

        fun applyToCommonSettings(commonSettings: CommonCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            commonSettings.apply {
                CALL_PARAMETERS_WRAP = CodeStyleSettings.DO_NOT_WRAP
                CALL_PARAMETERS_LPAREN_ON_NEXT_LINE = false
                CALL_PARAMETERS_RPAREN_ON_NEXT_LINE = false

                METHOD_PARAMETERS_WRAP = CodeStyleSettings.DO_NOT_WRAP
                METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE = false
                METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE = false

                EXTENDS_LIST_WRAP = CodeStyleSettings.DO_NOT_WRAP
                METHOD_CALL_CHAIN_WRAP = CodeStyleSettings.DO_NOT_WRAP
                ASSIGNMENT_WRAP = CodeStyleSettings.DO_NOT_WRAP
            }

            if (modifyCodeStyle && commonSettings is KotlinCommonCodeStyleSettings) {
                commonSettings.CODE_STYLE_DEFAULTS = CODE_STYLE_ID
            }
        }
    }
}
