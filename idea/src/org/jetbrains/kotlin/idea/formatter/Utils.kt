/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.util.module

fun PsiFile.commitAndUnblockDocument(): Boolean {
    val virtualFile = this.virtualFile ?: return false
    val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return false
    val documentManager = PsiDocumentManager.getInstance(project)
    documentManager.doPostponedOperationsAndUnblockDocument(document)
    documentManager.commitDocument(document)
    return true
}

fun PsiFile.adjustLineIndent(startOffset: Int, endOffset: Int) {
    if (!commitAndUnblockDocument()) return
    CodeStyleManager.getInstance(project).adjustLineIndent(this, TextRange(startOffset, endOffset))
}

fun trailingCommaAllowedInModule(source: PsiElement): Boolean =
    Registry.`is`("kotlin.formatter.allowTrailingCommaInAnyProject", false) ||
            source.module?.languageVersionSettings?.supportsFeature(LanguageFeature.TrailingCommas) == true
