/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KtFirFlexibleType(
    override val coneType: ConeFlexibleType,
    private val builder: KtSymbolByFirBuilder,
) : KtFlexibleType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token

    override val lowerBound: KtType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KtType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.upperBound) }
    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }
    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
