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

package org.jetbrains.kotlin.console

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.Colors
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Font

object ReplColors {
    val HISTORY_GUTTER_COLOR: JBColor = JBColor(Gray.xF2, Gray.x41)
    val EDITOR_GUTTER_COLOR: JBColor = JBColor(Gray.xCF, Gray.x31)
    val PLACEHOLDER_COLOR: JBColor = JBColor.LIGHT_GRAY

    val WARNING_INFO_CONTENT_TYPE: ConsoleViewContentType =
            ConsoleViewContentType(
                    "KOTLIN_CONSOLE_WARNING_INFO",
                    TextAttributes().apply { fontType = Font.ITALIC; foregroundColor = JBColor.RED }
            )
    val INITIAL_PROMPT_CONTENT_TYPE: ConsoleViewContentType =
            ConsoleViewContentType(
                    "KOTLIN_CONSOLE_INITIAL_PROMPT",
                    TextAttributes().apply { fontType = Font.BOLD }
            )
    val USER_OUTPUT_CONTENT_TYPE: ConsoleViewContentType =
            ConsoleViewContentType(
                    "KOTLIN_CONSOLE_USER_OUTPUT",
                    TextAttributes().apply { fontType = Font.ITALIC; foregroundColor = Colors.DARK_GREEN }
            )
}