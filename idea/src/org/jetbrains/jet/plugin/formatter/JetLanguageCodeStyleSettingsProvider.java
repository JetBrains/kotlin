/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.formatter;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

/**
 * @author Nikolay Krasko
 */
public class JetLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return
                "class Some {\n" +
                "  fun foo() : Int {\n" +
                "    val test : Int = 12\n" +
                "    return test\n" +
                "  }\n" +
                "}";
    }

    @Override
    public String getLanguageName() {
        return "Kotlin";
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showStandardOptions(
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    // "SPACE_AROUND_BITWISE_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                    // "SPACE_AROUND_SHIFT_OPERATORS",
                    "SPACE_AROUND_UNARY_OPERATOR",
                    "SPACE_AFTER_COMMA",
                    // "SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS",
                    "SPACE_BEFORE_COMMA"
                    // "SPACE_AFTER_SEMICOLON",
                    // "SPACE_BEFORE_SEMICOLON",
                    // "SPACE_WITHIN_PARENTHESES",
                    // "SPACE_WITHIN_METHOD_CALL_PARENTHESES",
                    // "SPACE_WITHIN_EMPTY_METHOD_CALL_PARENTHESES",
                    // "SPACE_WITHIN_METHOD_PARENTHESES",
                    // "SPACE_WITHIN_EMPTY_METHOD_PARENTHESES",
                    // "SPACE_WITHIN_IF_PARENTHESES",
                    // "SPACE_WITHIN_WHILE_PARENTHESES",
                    // "SPACE_WITHIN_FOR_PARENTHESES",
                    // "SPACE_WITHIN_TRY_PARENTHESES",
                    // "SPACE_WITHIN_CATCH_PARENTHESES",
                    // "SPACE_WITHIN_SWITCH_PARENTHESES",
                    // "SPACE_WITHIN_SYNCHRONIZED_PARENTHESES",
                    // "SPACE_WITHIN_CAST_PARENTHESES",
                    // "SPACE_WITHIN_BRACKETS",
                    // "SPACE_WITHIN_BRACES",
                    // "SPACE_WITHIN_ARRAY_INITIALIZER_BRACES",
                    // "SPACE_AFTER_TYPE_CAST",
                    // "SPACE_BEFORE_METHOD_CALL_PARENTHESES",
                    // "SPACE_BEFORE_METHOD_PARENTHESES",
                    // "SPACE_BEFORE_IF_PARENTHESES",
                    // "SPACE_BEFORE_WHILE_PARENTHESES",
                    // "SPACE_BEFORE_FOR_PARENTHESES",
                    // "SPACE_BEFORE_TRY_PARENTHESES",
                    // "SPACE_BEFORE_CATCH_PARENTHESES",
                    // "SPACE_BEFORE_SWITCH_PARENTHESES",
                    // "SPACE_BEFORE_SYNCHRONIZED_PARENTHESES",
                    // "SPACE_BEFORE_CLASS_LBRACE",
                    // "SPACE_BEFORE_METHOD_LBRACE",
                    // "SPACE_BEFORE_IF_LBRACE",
                    // "SPACE_BEFORE_ELSE_LBRACE",
                    // "SPACE_BEFORE_WHILE_LBRACE",
                    // "SPACE_BEFORE_FOR_LBRACE",
                    // "SPACE_BEFORE_DO_LBRACE",
                    // "SPACE_BEFORE_SWITCH_LBRACE",
                    // "SPACE_BEFORE_TRY_LBRACE",
                    // "SPACE_BEFORE_CATCH_LBRACE",
                    // "SPACE_BEFORE_FINALLY_LBRACE",
                    // "SPACE_BEFORE_SYNCHRONIZED_LBRACE",
                    // "SPACE_BEFORE_ARRAY_INITIALIZER_LBRACE",
                    // "SPACE_BEFORE_ANNOTATION_ARRAY_INITIALIZER_LBRACE",
                    // "SPACE_BEFORE_ELSE_KEYWORD",
                    // "SPACE_BEFORE_WHILE_KEYWORD",
                    // "SPACE_BEFORE_CATCH_KEYWORD",
                    // "SPACE_BEFORE_FINALLY_KEYWORD",
                    // "SPACE_BEFORE_QUEST",
                    // "SPACE_AFTER_QUEST",
                    // "SPACE_BEFORE_COLON",
                    // "SPACE_AFTER_COLON",
                    // "SPACE_BEFORE_TYPE_PARAMETER_LIST"
            );

            consumer.showCustomOption(JetCodeStyleSettings.class, "SPACE_AROUND_RANGE", "Around range (..)",
                                      CodeStyleSettingsCustomizable.SPACES_AROUND_OPERATORS);

            consumer.showCustomOption(JetCodeStyleSettings.class, "SPACE_AFTER_TYPE_COLON", "Space after colon, before declarations' type",
                                      CodeStyleSettingsCustomizable.SPACES_OTHER);

            consumer.showCustomOption(JetCodeStyleSettings.class, "SPACE_BEFORE_TYPE_COLON", "Space before colon, after declarations' name",
                                      CodeStyleSettingsCustomizable.SPACES_OTHER);
        } else {
            consumer.showAllStandardOptions();
        }


    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public boolean usesSharedPreview() {
        return false;
    }

    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings commonCodeStyleSettings = new CommonCodeStyleSettings(getLanguage());
        commonCodeStyleSettings.initIndentOptions();
        return commonCodeStyleSettings;
    }
}
