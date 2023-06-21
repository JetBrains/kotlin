/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.impl

import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.*

public object KtTypeRendererForSource {
    public val WITH_QUALIFIED_NAMES: KtTypeRenderer = KtTypeRenderer {
        capturedTypeRenderer = KtCapturedTypeRenderer.AS_PROJECTION
        definitelyNotNullTypeRenderer = KtDefinitelyNotNullTypeRenderer.AS_TYPE_INTERSECTION
        dynamicTypeRenderer = KtDynamicTypeRenderer.AS_DYNAMIC_WORD
        flexibleTypeRenderer = KtFlexibleTypeRenderer.AS_SHORT
        functionalTypeRenderer = KtFunctionalTypeRenderer.AS_CLASS_TYPE_FOR_REFLECTION_TYPES
        integerLiteralTypeRenderer = KtIntegerLiteralTypeRenderer.AS_ILT_WITH_VALUE
        intersectionTypeRenderer = KtIntersectionTypeRenderer.AS_INTERSECTION
        typeErrorTypeRenderer = KtTypeErrorTypeRenderer.AS_CODE_IF_POSSIBLE
        typeParameterTypeRenderer = KtTypeParameterTypeRenderer.AS_SOURCE
        unresolvedClassErrorTypeRenderer = KtUnresolvedClassErrorTypeRenderer.UNRESOLVED_QUALIFIER
        usualClassTypeRenderer = KtUsualClassTypeRenderer.AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS
        classIdRenderer = KtClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        typeNameRenderer = KtTypeNameRenderer.QUOTED
        typeApproximator = KtRendererTypeApproximator.TO_DENOTABLE
        typeProjectionRenderer = KtTypeProjectionRenderer.WITH_VARIANCE
        annotationsRenderer = KtAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        contextReceiversRenderer = KtContextReceiversRendererForSource.WITH_LABELS
        keywordsRenderer = KtKeywordsRenderer.AS_WORD
    }

    public val WITH_SHORT_NAMES: KtTypeRenderer = WITH_QUALIFIED_NAMES.with {
        classIdRenderer = KtClassTypeQualifierRenderer.WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS
        annotationsRenderer = KtAnnotationRendererForSource.WITH_SHORT_NAMES
    }
}