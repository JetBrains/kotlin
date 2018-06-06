/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.PredefinedCodeStyle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings

class KotlinStyleGuideCodeStyle : PredefinedCodeStyle("Kotlin style guide", KotlinLanguage.INSTANCE) {
    override fun apply(settings: CodeStyleSettings) {
        Companion.apply(settings)
    }

    companion object {
        const val CODE_STYLE_ID = "KOTLIN_OFFICIAL"
        const val CODE_STYLE_TITLE = "Kotlin Coding Conventions"

        fun apply(settings: CodeStyleSettings) {
            applyToKotlinCustomSettings(settings.kotlinCustomSettings)
            applyToCommonSettings(settings.kotlinCommonSettings)
        }

        fun applyToKotlinCustomSettings(kotlinCustomSettings: KotlinCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            kotlinCustomSettings.apply {
                if (modifyCodeStyle) {
                    CODE_STYLE_DEFAULTS = CODE_STYLE_ID
                }

                CONTINUATION_INDENT_IN_PARAMETER_LISTS = false
                CONTINUATION_INDENT_IN_ARGUMENT_LISTS = false
                CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = false
                CONTINUATION_INDENT_FOR_CHAINED_CALLS = false
                CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = false
                CONTINUATION_INDENT_IN_IF_CONDITIONS = false
                WRAP_EXPRESSION_BODY_FUNCTIONS = CodeStyleSettings.WRAP_AS_NEEDED
                IF_RPAREN_ON_NEW_LINE = true
            }
        }

        fun applyToCommonSettings(commonSettings: CommonCodeStyleSettings, modifyCodeStyle: Boolean = true) {
            commonSettings.apply {
                WHILE_ON_NEW_LINE = false
                ELSE_ON_NEW_LINE = false
                CATCH_ON_NEW_LINE = false
                FINALLY_ON_NEW_LINE = false

                CALL_PARAMETERS_WRAP = CodeStyleSettings.WRAP_AS_NEEDED + CodeStyleSettings.WRAP_ON_EVERY_ITEM
                CALL_PARAMETERS_LPAREN_ON_NEXT_LINE = true
                CALL_PARAMETERS_RPAREN_ON_NEXT_LINE = true

                METHOD_PARAMETERS_WRAP = CodeStyleSettings.WRAP_AS_NEEDED + CodeStyleSettings.WRAP_ON_EVERY_ITEM
                METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE = true
                METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE = true

                EXTENDS_LIST_WRAP = CodeStyleSettings.WRAP_AS_NEEDED
                METHOD_CALL_CHAIN_WRAP = CodeStyleSettings.WRAP_AS_NEEDED
                ASSIGNMENT_WRAP = CodeStyleSettings.WRAP_AS_NEEDED

                ALIGN_MULTILINE_BINARY_OPERATION = false
            }

            if (modifyCodeStyle && commonSettings is KotlinCommonCodeStyleSettings) {
                commonSettings.CODE_STYLE_DEFAULTS = CODE_STYLE_ID
            }
        }
    }
}
