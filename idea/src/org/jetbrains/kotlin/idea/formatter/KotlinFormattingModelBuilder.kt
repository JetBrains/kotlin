/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import com.intellij.psi.formatter.PsiBasedFormattingModel
import org.jetbrains.kotlin.idea.KotlinLanguage

class KotlinFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        val containingFile = element.containingFile.viewProvider.getPsi(KotlinLanguage.INSTANCE)
        val block = KotlinBlock(
            containingFile.node, NodeAlignmentStrategy.getNullStrategy(), Indent.getNoneIndent(), null, settings,
            createSpacingBuilder(settings, KotlinSpacingBuilderUtilImpl)
        )

        //TODO: this is temporary code to allow formatting non-physical files in non-UI thread (used by conversion from Java to Kotlin)
        // it's needed until IDEA's issue with this document being created with wrong threading policy is fixed
        if (!element.isPhysical) {
            val formattingDocumentModel =
                FormattingDocumentModelImpl(DocumentImpl(containingFile.viewProvider.contents, true), containingFile)
            return PsiBasedFormattingModel(containingFile, block, formattingDocumentModel)
        }

        if (element is PsiFile) {
            val collectChangesModel = createCollectFormattingChangesModel(element, block)
            if (collectChangesModel != null) {
                return collectChangesModel
            }
        }

        return FormattingModelProvider.createFormattingModelForPsiFile(element.containingFile, block, settings)
    }

    override fun getRangeAffectingIndent(psiFile: PsiFile, i: Int, astNode: ASTNode): TextRange? {
        return null
    }
}
