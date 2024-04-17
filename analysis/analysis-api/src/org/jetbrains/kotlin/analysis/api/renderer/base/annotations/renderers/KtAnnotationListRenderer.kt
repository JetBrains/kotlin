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
    context(KtAnalysisSession, KtAnnotationRenderer)
    public fun renderAnnotations(owner: KtAnnotated, printer: PrettyPrinter)

    public object FOR_SOURCE : KtAnnotationListRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderAnnotations(owner: KtAnnotated, printer: PrettyPrinter) {
            val annotations = owner.annotations.filter { annotationFilter.filter(it, owner) }.ifEmpty { return }
            printer.printCollection(
                annotations,
                separator = when (owner) {
                    is KtValueParameterSymbol -> " "
                    is KtDeclarationSymbol -> "\n"
                    else -> " "
                }
            ) { annotation ->
                append('@')
                annotationUseSiteTargetRenderer.renderUseSiteTarget(annotation, owner, printer)
                annotationsQualifiedNameRenderer.renderQualifier(annotation, owner, printer)
                annotationArgumentsRenderer.renderAnnotationArguments(annotation, owner, printer)
            }
        }
    }
}

