/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.fir.firResolveState
import org.jetbrains.kotlin.idea.fir.getOrBuildFirWithDiagnostics
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

class KotlinFirPsiChecker : AbstractKotlinPsiChecker() {
    override fun shouldHighlight(file: KtFile): Boolean {
        return true // todo
    }

    override fun annotateElement(element: PsiElement, containingFile: KtFile, holder: AnnotationHolder) {
        if (element !is KtElement) return
        val state = containingFile.firResolveState()
        containingFile.getOrBuildFirWithDiagnostics(state)

        val diagnostics = state.getDiagnostics(element)
        if (diagnostics.isEmpty()) return

        if (shouldHighlightErrors(element)) {
            highlightDiagnostics(diagnostics, holder)
        }
    }

    private fun highlightDiagnostics(diagnostics: Collection<Diagnostic>, holder: AnnotationHolder) {
        diagnostics.groupBy { it.factory }.forEach { group -> registerDiagnosticAnnotations(group.value, holder) }
    }

    private fun registerDiagnosticAnnotations(diagnostics: List<Diagnostic>, holder: AnnotationHolder) {
        assert(diagnostics.isNotEmpty())
        val diagnostic = diagnostics.first()

        val ranges = diagnostic.textRanges

        ranges.forEach { range ->
            diagnostics.forEach { diagnostic ->
                Diagnostic2Annotation.createAnnotation(
                    diagnostic,
                    range,
                    holder,
                    nonDefaultMessage = null,
                    textAttributes = null,
                    highlightType = null,
                    renderMessage = IdeErrorMessages::render
                )
            }
        }

    }

    private fun shouldHighlightErrors(element: KtElement): Boolean {
        return true // todo
    }
}