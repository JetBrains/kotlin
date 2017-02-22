/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings;

public class KotlinLanguageCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {
    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        switch (settingsType) {
            case WRAPPING_AND_BRACES_SETTINGS:
                return
                        "public class ThisIsASampleClass {\n" +
                        "    val test = \n" +
                        "        12\n" +
                        "\n" +
                        "    fun foo1(i1: Int, i2: Int, i3: Int) : Int {\n" +
                        "        when (i1) {\n" +
                        "            is Number -> 0\n" +
                        "            else -> 1\n" +
                        "        }\n" +
                        "        return 0\n" +
                        "    }\n" +
                        "    private fun foo2():Int {\n" +
                        "// todo: something\n" +
                        "        try {" +
                        "            return foo1(12, 13, 14)\n" +
                        "        }" +
                        "        catch (e: Exception) {" +
                        "            return 0" +
                        "        }" +
                        "        finally {" +
                        "           if (true) {" +
                        "               return 1" +
                        "           }" +
                        "           else {" +
                        "               return 2" +
                        "           }" +
                        "        }" +
                        "    }\n" +
                        "    private val f = {(a: Int)->a*2}\n" +
                        "}\n" +
                        "\n" +
                        "enum class Enumeration {\n" +
                        "    A,\n" +
                        "    B\n" +
                        "}\n" +
                        "";
            case BLANK_LINES_SETTINGS:
                return
                        "class Foo {\n" +
                        "    private var field1: Int = 1\n" +
                        "    private val field2: String? = null\n" +
                        "\n" +
                        "\n" +
                        "    init {\n" +
                        "        field1 = 2;\n" +
                        "    }\n" +
                        "\n" +
                        "    fun foo1() {\n" +
                        "        run {\n" +
                        "            \n" +
                        "            \n" +
                        "            \n" +
                        "            field1\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "\n" +
                        "    class InnerClass {\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "\n" +
                        "class AnotherClass {\n" +
                        "}\n" +
                        "\n" +
                        "interface TestInterface {\n" +
                        "}\n" +
                        "fun run(f: () -> Unit) {\n" +
                        "    f()\n" +
                        "}";
            default:
                return
                        "open class Some {\n"+
                        "    private val f: (Int)->Int = { (a: Int) -> a * 2 }\n"+
                        "    fun foo(): Int {\n"+
                        "        val test: Int = 12\n"+
                        "        for (i in 10..42) {\n"+
                        "            println (when {\n"+
                        "                i < test -> -1\n"+
                        "                i > test -> 1\n"+
                        "                else -> 0\n"+
                        "            })\n"+
                        "        }\n"+
                        "        return test\n"+
                        "    }\n"+
                        "    private fun <T>foo2(): Int where T : List<T> {\n"+
                        "        return 0\n"+
                        "    }\n"+
                        "}\n"+
                        "class AnotherClass<T : Any> : Some()\n";

        }
    }

    @Override
    public String getLanguageName() {
        return KotlinLanguage.NAME;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        switch (settingsType) {
            case SPACING_SETTINGS:
                consumer.showStandardOptions(
                        "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                        "SPACE_AROUND_LOGICAL_OPERATORS",
                        "SPACE_AROUND_EQUALITY_OPERATORS",
                        "SPACE_AROUND_RELATIONAL_OPERATORS",
                        "SPACE_AROUND_ADDITIVE_OPERATORS",
                        "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                        "SPACE_AROUND_UNARY_OPERATOR",
                        "SPACE_AFTER_COMMA",
                        "SPACE_BEFORE_COMMA"
                );

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_AROUND_RANGE", "Around range (..)",
                                          CodeStyleSettingsCustomizable.SPACES_AROUND_OPERATORS);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_BEFORE_TYPE_COLON",
                                          "Space before colon, after declarations' name",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_AFTER_TYPE_COLON",
                                          "Space after colon, before declarations' type",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_BEFORE_EXTEND_COLON",
                                          "Space before colon in new type definition",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_AFTER_EXTEND_COLON",
                                          "Space after colon in new type definition",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD",
                                          "Insert whitespaces in simple one line methods",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_AROUND_FUNCTION_TYPE_ARROW",
                                          "Surround arrow in function types with spaces",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_AROUND_WHEN_ARROW",
                                          "Surround arrow in \"when\" clause with spaces",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "SPACE_BEFORE_LAMBDA_ARROW",
                                          "Before lambda arrow",
                                          CodeStyleSettingsCustomizable.SPACES_OTHER);

                break;
            case WRAPPING_AND_BRACES_SETTINGS:
                consumer.showStandardOptions(
                        // "ALIGN_MULTILINE_CHAINED_METHODS",
                        "KEEP_FIRST_COLUMN_COMMENT",
                        "KEEP_LINE_BREAKS",
                        "ALIGN_MULTILINE_EXTENDS_LIST",
                        "ALIGN_MULTILINE_PARAMETERS",
                        "ALIGN_MULTILINE_PARAMETERS_IN_CALLS",
                        "ALIGN_MULTILINE_METHOD_BRACKETS",
                        "ALIGN_MULTILINE_BINARY_OPERATION",
                        "ELSE_ON_NEW_LINE",
                        "WHILE_ON_NEW_LINE",
                        "CATCH_ON_NEW_LINE",
                        "FINALLY_ON_NEW_LINE",
                        "CALL_PARAMETERS_WRAP",
                        "METHOD_PARAMETERS_WRAP"
                );
                consumer.renameStandardOption(CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT, "'when' statements");
                consumer.showCustomOption(KotlinCodeStyleSettings.class, "ALIGN_IN_COLUMNS_CASE_BRANCH", "Align in columns 'case' branches",
                                          CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT);

                consumer.showCustomOption(KotlinCodeStyleSettings.class, "LBRACE_ON_NEXT_LINE",
                                          "Put left brace on new line",
                                          CodeStyleSettingsCustomizable.WRAPPING_BRACES);
                break;
            case BLANK_LINES_SETTINGS:
                consumer.showStandardOptions(
                        "KEEP_BLANK_LINES_IN_CODE",
                        "KEEP_BLANK_LINES_IN_DECLARATIONS"
                );
                break;
            default:
                consumer.showStandardOptions();
                break;
        }
    }

    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings commonCodeStyleSettings = new CommonCodeStyleSettings(getLanguage());
        commonCodeStyleSettings.initIndentOptions();
        return commonCodeStyleSettings;
    }
}
