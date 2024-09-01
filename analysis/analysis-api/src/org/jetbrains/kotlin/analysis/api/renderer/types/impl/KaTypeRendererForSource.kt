/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.*

@KaExperimentalApi
public object KaTypeRendererForSource {
    public val WITH_QUALIFIED_NAMES: KaTypeRenderer = KaTypeRenderer {
        expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE
        capturedTypeRenderer = KaCapturedTypeRenderer.AS_PROJECTION
        definitelyNotNullTypeRenderer = KaDefinitelyNotNullTypeRenderer.AS_TYPE_INTERSECTION
        dynamicTypeRenderer = KaDynamicTypeRenderer.AS_DYNAMIC_WORD
        flexibleTypeRenderer = KaFlexibleTypeRenderer.AS_SHORT
        functionalTypeRenderer = KaFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES
        intersectionTypeRenderer = KaIntersectionTypeRenderer.AS_INTERSECTION
        errorTypeRenderer = KaErrorTypeRenderer.AS_CODE_IF_POSSIBLE
        typeParameterTypeRenderer = KaTypeParameterTypeRenderer.AS_SOURCE
        unresolvedClassErrorTypeRenderer = KaUnresolvedClassErrorTypeRenderer.UNRESOLVED_QUALIFIER
        usualClassTypeRenderer = KaUsualClassTypeRenderer.AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS
        classIdRenderer = KaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        typeNameRenderer = KaTypeNameRenderer.QUOTED
        typeApproximator = KaRendererTypeApproximator.TO_DENOTABLE
        typeProjectionRenderer = KaTypeProjectionRenderer.WITH_VARIANCE
        annotationsRenderer = KaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        contextReceiversRenderer = KaContextReceiversRendererForSource.WITH_LABELS
        keywordsRenderer = KaKeywordsRenderer.AS_WORD
    }

    public val WITH_SHORT_NAMES: KaTypeRenderer = WITH_QUALIFIED_NAMES.with {
        classIdRenderer = KaClassTypeQualifierRenderer.WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS
        annotationsRenderer = KaAnnotationRendererForSource.WITH_SHORT_NAMES
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaTypeRendererForSource' instead", ReplaceWith("KaTypeRendererForSource"))
public typealias KtTypeRendererForSource = KaTypeRendererForSource