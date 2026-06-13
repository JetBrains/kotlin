/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

@KaSpi
@KaExperimentalApi
public interface KaAnnotationUseSiteTargetRenderer {
    public fun renderUseSiteTarget(
        analysisSession: KaSession,
        annotation: KaAnnotation,
        owner: KaAnnotated,
        annotationRenderer: KaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object WITHOUT_USE_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    @KaExperimentalApi
    public object WITH_USES_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val name = when (owner) {
                is KaReceiverParameterSymbol -> AnnotationUseSiteTarget.RECEIVER.renderName
                else -> return
            }

            printer.append(name)
            printer.append(':')
        }
    }

    @KaExperimentalApi
    public object WITH_NON_DEFAULT_USE_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            WITH_USES_SITE.renderUseSiteTarget(analysisSession, annotation, owner, annotationRenderer, printer)
        }
    }
}
