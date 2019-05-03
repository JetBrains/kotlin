/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.Block
import com.intellij.formatting.FormattingDocumentModel
import com.intellij.formatting.FormattingModel
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import org.jetbrains.kotlin.idea.formatter.FormattingChange.ReplaceWhiteSpace
import org.jetbrains.kotlin.idea.formatter.FormattingChange.ShiftIndentInsideRange
import org.jetbrains.kotlin.psi.NotNullablePsiCopyableUserDataProperty
import org.jetbrains.kotlin.psi.UserDataProperty

private var PsiFile.collectFormattingChanges: Boolean by NotNullablePsiCopyableUserDataProperty(Key.create("COLLECT_FORMATTING_CHANGES"), false)
private var PsiFile.collectChangesFormattingModel: CollectChangesWithoutApplyModel? by UserDataProperty(Key.create("COLLECT_CHANGES_FORMATTING_MODEL"))

fun createCollectFormattingChangesModel(file: PsiFile, block: Block): FormattingModel? {
    if (file.collectFormattingChanges) {
        return CollectChangesWithoutApplyModel(file, block).also {
            file.collectChangesFormattingModel = it
        }
    }

    return null
}

sealed class FormattingChange {
    data class ShiftIndentInsideRange(val node: ASTNode?, val range: TextRange, val indent: Int) : FormattingChange()
    data class ReplaceWhiteSpace(val textRange: TextRange, val whiteSpace: String) : FormattingChange()
}

fun collectFormattingChanges(file: PsiFile): Set<FormattingChange> {
    try {
        file.collectFormattingChanges = true
        CodeStyleManager.getInstance(file.project).reformat(file, true)
        return file.collectChangesFormattingModel?.requestedChanges ?: emptySet()
    }
    finally {
        file.collectFormattingChanges = false
        file.collectChangesFormattingModel = null
    }
}

private class CollectChangesWithoutApplyModel(val file: PsiFile, val block: Block) : FormattingModel {
    private val documentModel = FormattingDocumentModelImpl(DocumentImpl(file.viewProvider.contents, true), file)
    private val changes = HashSet<FormattingChange>()

    val requestedChanges: Set<FormattingChange> get() = changes

    override fun commitChanges() {
        /* do nothing */
    }

    override fun getDocumentModel(): FormattingDocumentModel = documentModel
    override fun getRootBlock(): Block = block

    override fun shiftIndentInsideRange(node: ASTNode?, range: TextRange, indent: Int): TextRange {
        changes.add(ShiftIndentInsideRange(node, range, indent))
        return range
    }

    override fun replaceWhiteSpace(textRange: TextRange, whiteSpace: String): TextRange {
        changes.add(ReplaceWhiteSpace(textRange, whiteSpace))
        return textRange
    }
}