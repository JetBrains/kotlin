/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KtAnnotationUseSiteTargetRenderer

public object KtAnnotationRendererForSource {
    public val WITH_QUALIFIED_NAMES: KtAnnotationRenderer = KtAnnotationRenderer {
        annotationListRenderer = KtAnnotationListRenderer.FOR_SOURCE
        annotationFilter = KtRendererAnnotationsFilter.NO_NULLABILITY and KtRendererAnnotationsFilter.NO_PARAMETER_NAME
        annotationsQualifiedNameRenderer = KtAnnotationQualifierRenderer.WITH_QUALIFIED_NAMES
        annotationUseSiteTargetRenderer = KtAnnotationUseSiteTargetRenderer.WITH_NON_DEFAULT_USE_SITE
        annotationArgumentsRenderer = KtAnnotationArgumentsRenderer.IF_ANY
    }

    public val WITH_SHORT_NAMES: KtAnnotationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationsQualifiedNameRenderer = KtAnnotationQualifierRenderer.WITH_SHORT_NAMES
    }
}
