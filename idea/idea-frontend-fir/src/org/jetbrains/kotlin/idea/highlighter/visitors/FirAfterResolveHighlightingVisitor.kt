/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter.visitors

import com.intellij.lang.annotation.AnnotationHolder
import org.jetbrains.kotlin.idea.frontend.api.FrontendAnalysisSession
import org.jetbrains.kotlin.idea.highlighter.HighlightingVisitor

abstract class FirAfterResolveHighlightingVisitor(
    protected val analysisSession: FrontendAnalysisSession,
    protected val holder: AnnotationHolder
) : HighlightingVisitor(holder) {

    companion object {
        fun createListOfVisitors(
            analysisSession: FrontendAnalysisSession,
            holder: AnnotationHolder
        ): List<FirAfterResolveHighlightingVisitor> = listOf(
            TypeHighlightingVisitor(analysisSession, holder),
            FunctionCallHighlightingVisitor(analysisSession, holder),
            ExpressionsSmartcastHighlightingVisitor(analysisSession, holder),
            VariableReferenceHighlightingVisitor(analysisSession, holder),
        )
    }
}