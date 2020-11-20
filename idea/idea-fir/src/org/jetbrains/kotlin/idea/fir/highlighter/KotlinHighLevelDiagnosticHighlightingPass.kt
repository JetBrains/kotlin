/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.highlighter.Diagnostic2Annotation
import org.jetbrains.kotlin.idea.highlighter.IdeErrorMessages
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtVisitorVoid

class KotlinHighLevelDiagnosticHighlightingPass(
    private val ktFile: KtFile,
    document: Document,
) : TextEditorHighlightingPass(ktFile.project, document) {
    private val diagnosticInfos = mutableListOf<HighlightInfo>()

    override fun doCollectInformation(progress: ProgressIndicator) = analyze(ktFile) {
        ktFile.collectDiagnosticsForFile().forEach { diagnostic ->
            if (!diagnostic.isValid) return@forEach
            diagnostic.textRanges.forEach { range ->
                HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR/*TODO*/)
                    .descriptionAndTooltip(Diagnostic2Annotation.getMessage(diagnostic, IdeErrorMessages::render))
                    .range(range)
                    .create()
                    ?.let(diagnosticInfos::add)
            }
        }
    }


    override fun doApplyInformationToEditor() {
        UpdateHighlightersUtil.setHighlightersToEditor(
            myProject, myDocument!!, /*startOffset=*/0, ktFile.textLength, diagnosticInfos, colorsScheme, id
        )
    }
}

internal class KotlinDiagnosticHighlightingPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (file !is KtFile) return null
        if (file.isCompiled) return null
        return KotlinHighLevelDiagnosticHighlightingPass(file, editor.document)
    }
}
