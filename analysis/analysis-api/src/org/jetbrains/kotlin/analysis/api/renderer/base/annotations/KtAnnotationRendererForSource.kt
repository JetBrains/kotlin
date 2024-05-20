/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations

import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationArgumentsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationQualifierRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers.KaAnnotationUseSiteTargetRenderer

public object KaAnnotationRendererForSource {
    public val WITH_QUALIFIED_NAMES: KaAnnotationRenderer = KaAnnotationRenderer {
        annotationListRenderer = KaAnnotationListRenderer.FOR_SOURCE
        annotationFilter = KaRendererAnnotationsFilter.NO_NULLABILITY and KaRendererAnnotationsFilter.NO_PARAMETER_NAME
        annotationsQualifiedNameRenderer = KaAnnotationQualifierRenderer.WITH_QUALIFIED_NAMES
        annotationUseSiteTargetRenderer = KaAnnotationUseSiteTargetRenderer.WITH_NON_DEFAULT_USE_SITE
        annotationArgumentsRenderer = KaAnnotationArgumentsRenderer.IF_ANY
    }

    public val WITH_SHORT_NAMES: KaAnnotationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationsQualifiedNameRenderer = KaAnnotationQualifierRenderer.WITH_SHORT_NAMES
    }
}

public typealias KtAnnotationRendererForSource = KaAnnotationRendererForSource