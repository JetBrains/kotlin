/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.impl.source.codeStyle.lineIndent.FormatterBasedLineIndentProvider
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinLineIndentProvider : LineIndentProvider {
    private val formatterBasedProvider = FormatterBasedLineIndentProvider()

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        val lineIndent = formatterBasedProvider.getLineIndent(project, editor, language, offset)
        return if (useFormatter) lineIndent else lineIndent
    }

    override fun isSuitableFor(language: Language?): Boolean = language?.isKindOf(KotlinLanguage.INSTANCE) == true

    companion object {
        @get:TestOnly
        @set:TestOnly
        internal var useFormatter: Boolean = false
    }
}