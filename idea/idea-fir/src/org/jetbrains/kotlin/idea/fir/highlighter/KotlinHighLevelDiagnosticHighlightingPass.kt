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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.getDefaultMessageWithFactoryName
import org.jetbrains.kotlin.psi.KtFile

class KotlinHighLevelDiagnosticHighlightingPass(
    private val ktFile: KtFile,
    document: Document,
) : TextEditorHighlightingPass(ktFile.project, document) {
    private val diagnosticInfos = mutableListOf<HighlightInfo>()

    override fun doCollectInformation(progress: ProgressIndicator) = analyze(ktFile) {
        ktFile.collectDiagnosticsForFile().forEach { diagnostic ->
            diagnostic.textRanges.forEach { range ->
                HighlightInfo.newHighlightInfo(HighlightInfoType.ERROR/*TODO*/)
                    .descriptionAndTooltip(diagnostic.getMessageToRender())
                    .range(range)
                    .create()
                    ?.let(diagnosticInfos::add)
            }
        }
    }

    private fun KtDiagnostic.getMessageToRender(): String =
        if (isInternalOrUnitTestMode())
            getDefaultMessageWithFactoryName()
        else defaultMessage

    private fun isInternalOrUnitTestMode(): Boolean {
        val application = ApplicationManager.getApplication()
        return application.isInternal || application.isUnitTestMode
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
