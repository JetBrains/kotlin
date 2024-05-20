/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationUseSiteTargetRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KaAnnotationRenderer internal constructor(
    public val annotationListRenderer: KaAnnotationListRenderer,
    public val annotationFilter: KaRendererAnnotationsFilter,
    public val annotationsQualifiedNameRenderer: KaAnnotationQualifierRenderer,
    public val annotationUseSiteTargetRenderer: KaAnnotationUseSiteTargetRenderer,
    public val annotationArgumentsRenderer: KaAnnotationArgumentsRenderer,
) {
    public fun renderAnnotations(analysisSession: KaSession, owner: KaAnnotated, printer: PrettyPrinter) {
        annotationListRenderer.renderAnnotations(analysisSession, owner, this, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KaAnnotationRenderer {
        val renderer = this
        return KaAnnotationRenderer {
            this.annotationListRenderer = renderer.annotationListRenderer
            this.annotationFilter = renderer.annotationFilter
            this.annotationsQualifiedNameRenderer = renderer.annotationsQualifiedNameRenderer
            this.annotationUseSiteTargetRenderer = renderer.annotationUseSiteTargetRenderer
            this.annotationArgumentsRenderer = renderer.annotationArgumentsRenderer

            action()
        }
    }

    public class Builder {
        public lateinit var annotationListRenderer: KaAnnotationListRenderer
        public lateinit var annotationFilter: KaRendererAnnotationsFilter
        public lateinit var annotationsQualifiedNameRenderer: KaAnnotationQualifierRenderer
        public lateinit var annotationUseSiteTargetRenderer: KaAnnotationUseSiteTargetRenderer
        public lateinit var annotationArgumentsRenderer: KaAnnotationArgumentsRenderer


        public fun build(): KaAnnotationRenderer = KaAnnotationRenderer(
            annotationListRenderer,
            annotationFilter,
            annotationsQualifiedNameRenderer,
            annotationUseSiteTargetRenderer,
            annotationArgumentsRenderer
        )

    }

    public companion object {
        public inline operator fun invoke(action: Builder.() -> Unit): KaAnnotationRenderer =
            Builder().apply(action).build()
    }
}

public typealias KtAnnotationRenderer = KaAnnotationRenderer