/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaAnnotationListRenderer {
    public fun renderAnnotations(
        analysisSession: KaSession,
        owner: KaAnnotated,
        annotationRenderer: KaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object FOR_SOURCE : KaAnnotationListRenderer {
        override fun renderAnnotations(
            analysisSession: KaSession,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val annotations = owner.annotations
                .filter { annotationRenderer.annotationFilter.filter(analysisSession, it, owner) }
                .ifEmpty { return }

            printer.printCollection(
                annotations,
                separator = when (owner) {
                    is KaValueParameterSymbol -> " "
                    is KaDeclarationSymbol -> "\n"
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

public typealias KtAnnotationListRenderer = KaAnnotationListRenderer