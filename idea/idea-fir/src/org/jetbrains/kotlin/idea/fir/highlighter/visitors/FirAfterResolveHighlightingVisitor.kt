/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.highlighter.AbstractAnnotationHolderHighlightingVisitor

abstract class FirAfterResolveHighlightingVisitor(
    protected val analysisSession: KtAnalysisSession,
    protected val holder: AnnotationHolder
) : AbstractAnnotationHolderHighlightingVisitor(holder) {

    override fun createInfoAnnotation(textRange: TextRange, message: String?, textAttributes: TextAttributesKey?) {
        // TODO: Temporary use deprecated for FIR plugin as it is supposes to be rewritten fully
        holder.createInfoAnnotation(textRange, message)
            .also { annotation -> textAttributes?.let { annotation.textAttributes = textAttributes } }
    }

    companion object {
        fun createListOfVisitors(
            analysisSession: KtAnalysisSession,
            holder: AnnotationHolder
        ): List<FirAfterResolveHighlightingVisitor> = listOf(
            TypeHighlightingVisitor(analysisSession, holder),
            FunctionCallHighlightingVisitor(analysisSession, holder),
            ExpressionsSmartcastHighlightingVisitor(analysisSession, holder),
            VariableReferenceHighlightingVisitor(analysisSession, holder),
        )
    }
}