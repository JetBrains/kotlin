/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.EditorTestFixture


class Fixture(val project: Project, val editor: Editor, val vFile: VirtualFile) {
    private val delegate = EditorTestFixture(project, editor, vFile)

    val document: Document
        get() = editor.document

    fun doHighlighting(): List<HighlightInfo> = delegate.doHighlighting() ?: emptyList()

    fun type(s: String) {
        delegate.type(s)
    }

    fun complete(type: CompletionType = CompletionType.BASIC, invocationCount: Int = 1): Array<LookupElement> =
        delegate.complete(type, invocationCount) ?: emptyArray()
}
