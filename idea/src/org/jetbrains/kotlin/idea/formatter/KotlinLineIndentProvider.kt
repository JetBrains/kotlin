/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyle
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.formatter.lineIndent.KotlinIndentationAdjuster
import org.jetbrains.kotlin.idea.formatter.lineIndent.KotlinLikeLangLineIndentProvider

class KotlinLineIndentProvider : KotlinLikeLangLineIndentProvider() {
    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? =
        if (useFormatter)
            null
        else
            super.getLineIndent(project, editor, language, offset)

    override fun indentionSettings(project: Project): KotlinIndentationAdjuster = object : KotlinIndentationAdjuster {
        val settings = CodeStyle.getSettings(project)

        override val alignWhenMultilineFunctionParentheses: Boolean
            get() = settings.kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS

        override val alignWhenMultilineBinaryExpression: Boolean
            get() = settings.kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION
    }

    companion object {
        @get:TestOnly
        @set:TestOnly
        internal var useFormatter: Boolean = false
    }
}