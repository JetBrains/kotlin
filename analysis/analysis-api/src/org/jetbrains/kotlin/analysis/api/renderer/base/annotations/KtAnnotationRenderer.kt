/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationUseSiteTargetRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KtAnnotationRenderer internal constructor(
    public val annotationListRenderer: KtAnnotationListRenderer,
    public val annotationFilter: KtRendererAnnotationsFilter,
    public val annotationsQualifiedNameRenderer: KtAnnotationQualifierRenderer,
    public val annotationUseSiteTargetRenderer: KtAnnotationUseSiteTargetRenderer,
    public val annotationArgumentsRenderer: KtAnnotationArgumentsRenderer,
) {
    public fun renderAnnotations(analysisSession: KtAnalysisSession, owner: KtAnnotated, printer: PrettyPrinter) {
        annotationListRenderer.renderAnnotations(analysisSession, owner, this, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KtAnnotationRenderer {
        val renderer = this
        return KtAnnotationRenderer {
            this.annotationListRenderer = renderer.annotationListRenderer
            this.annotationFilter = renderer.annotationFilter
            this.annotationsQualifiedNameRenderer = renderer.annotationsQualifiedNameRenderer
            this.annotationUseSiteTargetRenderer = renderer.annotationUseSiteTargetRenderer
            this.annotationArgumentsRenderer = renderer.annotationArgumentsRenderer

            action()
        }
    }

    public class Builder {
        public lateinit var annotationListRenderer: KtAnnotationListRenderer
        public lateinit var annotationFilter: KtRendererAnnotationsFilter
        public lateinit var annotationsQualifiedNameRenderer: KtAnnotationQualifierRenderer
        public lateinit var annotationUseSiteTargetRenderer: KtAnnotationUseSiteTargetRenderer
        public lateinit var annotationArgumentsRenderer: KtAnnotationArgumentsRenderer


        public fun build(): KtAnnotationRenderer = KtAnnotationRenderer(
            annotationListRenderer,
            annotationFilter,
            annotationsQualifiedNameRenderer,
            annotationUseSiteTargetRenderer,
            annotationArgumentsRenderer
        )

    }

    public companion object {
        public inline operator fun invoke(action: Builder.() -> Unit): KtAnnotationRenderer =
            Builder().apply(action).build()
    }
}
