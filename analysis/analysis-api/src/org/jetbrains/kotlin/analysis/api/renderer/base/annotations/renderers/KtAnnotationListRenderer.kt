/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtAnnotationListRenderer {
    public fun renderAnnotations(
        analysisSession: KtAnalysisSession,
        owner: KtAnnotated,
        annotationRenderer: KtAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object FOR_SOURCE : KtAnnotationListRenderer {
        override fun renderAnnotations(
            analysisSession: KtAnalysisSession,
            owner: KtAnnotated,
            annotationRenderer: KtAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val annotations = owner.annotations
                .filter { annotationRenderer.annotationFilter.filter(analysisSession, it, owner) }
                .ifEmpty { return }

            printer.printCollection(
                annotations,
                separator = when (owner) {
                    is KtValueParameterSymbol -> " "
                    is KtDeclarationSymbol -> "\n"
                    else -> " "
                }
            ) { annotation ->
                append('@')

                annotationRenderer.annotationUseSiteTargetRenderer
                    .renderUseSiteTarget(analysisSession, annotation, owner, annotationRenderer, printer)

                annotationRenderer.annotationsQualifiedNameRenderer
                    .renderQualifier(analysisSession, annotation, owner, annotationRenderer, printer)

                annotationRenderer.annotationArgumentsRenderer
                    .renderAnnotationArguments(analysisSession, annotation, owner, annotationRenderer, printer)
            }
        }
    }
}

