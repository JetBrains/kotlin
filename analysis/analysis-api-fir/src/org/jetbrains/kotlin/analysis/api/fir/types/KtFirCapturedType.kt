/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtCapturedType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KtFirCapturedType(
    override val coneType: ConeCapturedType,
    private val builder: KtSymbolByFirBuilder,
) : KtCapturedType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val projection: KtTypeProjection
        get() = withValidityAssertion { builder.typeBuilder.buildTypeProjection(coneType.constructor.projection) }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }


    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
