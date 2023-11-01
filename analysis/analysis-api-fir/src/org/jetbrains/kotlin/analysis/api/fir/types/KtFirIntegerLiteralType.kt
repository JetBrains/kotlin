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
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtIntegerLiteralType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralConstantType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KtFirIntegerLiteralType(
    override val coneType: ConeIntegerLiteralConstantType,
    private val builder: KtSymbolByFirBuilder,
) : KtIntegerLiteralType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token

    override val isUnsigned: Boolean get() = withValidityAssertion { coneType.isUnsigned }

    override val value: Long get() = withValidityAssertion { coneType.value }

    override val possibleTypes: List<KtClassType> by cached {
        coneType.possibleTypes.map { possibleType ->
            builder.typeBuilder.buildKtType(possibleType) as KtClassType
        }
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
