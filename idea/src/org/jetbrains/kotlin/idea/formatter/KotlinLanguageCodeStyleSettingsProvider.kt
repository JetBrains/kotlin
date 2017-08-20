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

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.IndentOptionsEditor
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import kotlin.reflect.KProperty

class KotlinLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = KotlinLanguage.INSTANCE

    override fun getCodeSample(settingsType: LanguageCodeStyleSettingsProvider.SettingsType): String = when (settingsType) {
        LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS ->
            """
               @Deprecated("Foo") public class ThisIsASampleClass : Comparable<*>, Appendable {
                   val test =
                       12

                   @Deprecated("Foo") fun foo1(i1: Int, i2: Int, i3: Int) : Int {
                       when (i1) {
                           is Number -> 0
                           else -> 1
                       }
                       return 0
                   }
                   private fun foo2():Int {
               // todo: something
                       try {            return foo1(12, 13, 14)
                       }        catch (e: Exception) {            return 0        }        finally {           if (true) {               return 1           }           else {               return 2           }        }    }
                   private val f = {(a: Int)->a*2}

                   fun longMethod(@Named("param1") param1: Int,
                    param2: String) {
                   }
               }

               enum class Enumeration {
                   A,
                   B
               }
            """.trimIndent()

        LanguageCodeStyleSettingsProvider.SettingsType.BLANK_LINES_SETTINGS ->
            """
                class Foo {
                   private var field1: Int = 1
                   private val field2: String? = null


                   init {
                       field1 = 2;
                   }

                   fun foo1() {
                       run {



                           field1
                       }

                       when(field1) {
                           1 -> println("1")
                           2 -> println("2")
                           3 ->
                                println("3" +
                                     "4")
                       }

                       when(field2) {
                           1 -> {
                               println("1")
                           }

                           2 -> {
                               println("2")
                           }
                       }
                   }


                   class InnerClass {
                   }
               }



               class AnotherClass {
               }

               interface TestInterface {
               }
               fun run(f: () -> Unit) {
                   f()
               }""".trimIndent()

        else -> """open class Some {
                       private val f: (Int)->Int = { (a: Int) -> a * 2 }
                       fun foo(): Int {
                           val test: Int = 12
                           for (i in 10..42) {
                               println (when {
                                   i < test -> -1
                                   i > test -> 1
                                   else -> 0
                               })
                           }
                           if (true) { }
                           while (true) { break }
                           try {
                               when (test) {
                                   12 -> println("foo")
                                   else -> println("bar")
                               }
                           } catch (e: Exception) {
                           } finally {
                           }
                           return test
                       }
                       private fun <T>foo2(): Int where T : List<T> {
                           return 0
                       }

                       fun multilineMethod(
                           foo: String,
                           bar: String
                       ) {
                           foo
                               .length
                       }

                       fun expressionBodyMethod() =
                               "abc"
                   }
                   class AnotherClass<T : Any> : Some()
                   """.trimIndent()
    }

    override fun getLanguageName(): String = KotlinLanguage.NAME

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: LanguageCodeStyleSettingsProvider.SettingsType) {
        fun showCustomOption(field: KProperty<*>, title: String, groupName: String? = null) {
            consumer.showCustomOption(KotlinCodeStyleSettings::class.java, field.name, title, groupName)
        }

        when (settingsType) {
            LanguageCodeStyleSettingsProvider.SettingsType.SPACING_SETTINGS -> {
                consumer.showStandardOptions(
                        "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                        "SPACE_AROUND_LOGICAL_OPERATORS",
                        "SPACE_AROUND_EQUALITY_OPERATORS",
                        "SPACE_AROUND_RELATIONAL_OPERATORS",
                        "SPACE_AROUND_ADDITIVE_OPERATORS",
                        "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                        "SPACE_AROUND_UNARY_OPERATOR",
                        "SPACE_AFTER_COMMA",
                        "SPACE_BEFORE_COMMA",
                        "SPACE_BEFORE_IF_PARENTHESES",
                        "SPACE_BEFORE_WHILE_PARENTHESES",
                        "SPACE_BEFORE_FOR_PARENTHESES",
                        "SPACE_BEFORE_CATCH_PARENTHESES"
                );

                showCustomOption(KotlinCodeStyleSettings::SPACE_AROUND_RANGE,
                                 "Range operator (..)",
                                 CodeStyleSettingsCustomizable.SPACES_AROUND_OPERATORS)

                showCustomOption(KotlinCodeStyleSettings::SPACE_BEFORE_TYPE_COLON,
                                 "Before colon, after declaration name",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_AFTER_TYPE_COLON,
                                 "After colon, before declaration type",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_BEFORE_EXTEND_COLON,
                                 "Before colon in new type definition",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_AFTER_EXTEND_COLON,
                                "After colon in new type definition",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD,
                                 "In simple one line methods",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_AROUND_FUNCTION_TYPE_ARROW,
                                 "Around arrow in function types",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_AROUND_WHEN_ARROW,
                                "Around arrow in \"when\" clause",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_BEFORE_LAMBDA_ARROW,
                                "Before lambda arrow",
                                 CodeStyleSettingsCustomizable.SPACES_OTHER)

                showCustomOption(KotlinCodeStyleSettings::SPACE_BEFORE_WHEN_PARENTHESES,
                                 "'when' parentheses",
                                 CodeStyleSettingsCustomizable.SPACES_BEFORE_PARENTHESES)
            }
                LanguageCodeStyleSettingsProvider.SettingsType.WRAPPING_AND_BRACES_SETTINGS -> {
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
                        "METHOD_PARAMETERS_WRAP",
                        "EXTENDS_LIST_WRAP",
                        "METHOD_ANNOTATION_WRAP",
                        "CLASS_ANNOTATION_WRAP",
                        "PARAMETER_ANNOTATION_WRAP",
                        "METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE",
                        "METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE",
                        "CALL_PARAMETERS_LPAREN_ON_NEXT_LINE",
                        "CALL_PARAMETERS_RPAREN_ON_NEXT_LINE"
                )
                consumer.renameStandardOption(CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT, "'when' statements")
                showCustomOption(KotlinCodeStyleSettings::ALIGN_IN_COLUMNS_CASE_BRANCH,
                                 "Align 'when' branches in columns",
                                 CodeStyleSettingsCustomizable.WRAPPING_SWITCH_STATEMENT)

                showCustomOption(KotlinCodeStyleSettings::LBRACE_ON_NEXT_LINE,
                                "Put left brace on new line",
                                 CodeStyleSettingsCustomizable.WRAPPING_BRACES)
            }
            LanguageCodeStyleSettingsProvider.SettingsType.BLANK_LINES_SETTINGS -> {
                consumer.showStandardOptions(
                        "KEEP_BLANK_LINES_IN_CODE",
                        "KEEP_BLANK_LINES_IN_DECLARATIONS",
                        "KEEP_BLANK_LINES_BEFORE_RBRACE",
                        "BLANK_LINES_AFTER_CLASS_HEADER"
                )
                showCustomOption(KotlinCodeStyleSettings::BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES,
                                 "Around 'when' branches with {}",
                                 CodeStyleSettingsCustomizable.BLANK_LINES)
            }
            else -> consumer.showStandardOptions()
        }
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = KotlinIndentOptionsEditor()

    override fun getDefaultCommonSettings(): CommonCodeStyleSettings =
        CommonCodeStyleSettings(language).apply {
            initIndentOptions()
        }
}
