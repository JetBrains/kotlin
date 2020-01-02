/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    val WARNING_INFO_CONTENT_TYPE: ConsoleViewContentType = ConsoleViewContentType(
        "KOTLIN_CONSOLE_WARNING_INFO",
        TextAttributes().apply { fontType = Font.ITALIC; foregroundColor = JBColor.RED }
    )

    val INITIAL_PROMPT_CONTENT_TYPE: ConsoleViewContentType = ConsoleViewContentType(
        "KOTLIN_CONSOLE_INITIAL_PROMPT",
        TextAttributes().apply { fontType = Font.BOLD }
    )

    val USER_OUTPUT_CONTENT_TYPE: ConsoleViewContentType = ConsoleViewContentType(
        "KOTLIN_CONSOLE_USER_OUTPUT",
        TextAttributes().apply { fontType = Font.ITALIC; foregroundColor = Colors.DARK_GREEN }
    )
}