/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.lang.annotation.AnnotationBuilder
import com.intellij.lang.annotation.AnnotationSession
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.getDefaultMessageWithFactoryName
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KtFirDiagnostic
import org.jetbrains.kotlin.idea.fir.api.fixes.KtQuickFixService
import org.jetbrains.kotlin.idea.frontend.api.components.KtDiagnosticCheckerFilter
import org.jetbrains.kotlin.psi.KtFile

class KotlinHighLevelDiagnosticHighlightingPass(
    private val ktFile: KtFile,
    document: Document,
) : TextEditorHighlightingPass(ktFile.project, document) {

    @Suppress("UnstableApiUsage")
    val annotationHolder = AnnotationHolderImpl(AnnotationSession(ktFile))

    override fun doCollectInformation(progress: ProgressIndicator) {
        analyze(ktFile) {
            ktFile.collectDiagnosticsForFile(KtDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS).forEach { diagnostic ->
                addDiagnostic(diagnostic)
            }
        }
    }

    private fun KtAnalysisSession.addDiagnostic(diagnostic: KtDiagnosticWithPsi<*>) {
        val fixes = with(service<KtQuickFixService>()) { getQuickFixesFor(diagnostic as KtFirDiagnostic) }
        annotationHolder.runAnnotatorWithContext(diagnostic.psi) { _, _ ->
            diagnostic.textRanges.forEach { range ->
                annotationHolder.newAnnotation(diagnostic.getHighlightSeverity(), diagnostic.getMessageToRender())
                    .addFixes(fixes)
                    .range(range)
                    .create()
            }
        }
    }

    private fun KtDiagnosticWithPsi<*>.getHighlightSeverity() = when (severity) {
        Severity.INFO -> HighlightSeverity.INFORMATION
        Severity.ERROR -> HighlightSeverity.ERROR
        Severity.WARNING -> HighlightSeverity.WARNING
    }


    private fun AnnotationBuilder.addFixes(fixes: List<IntentionAction>) =
        fixes.fold(this, AnnotationBuilder::withFix)

    private fun KtDiagnostic.getMessageToRender(): String =
        if (isInternalOrUnitTestMode())
            getDefaultMessageWithFactoryName()
        else defaultMessage

    private fun isInternalOrUnitTestMode(): Boolean {
        val application = ApplicationManager.getApplication()
        return application.isInternal || application.isUnitTestMode
    }


    override fun doApplyInformationToEditor() {
        val diagnosticInfos = annotationHolder.map(HighlightInfo::fromAnnotation)
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
