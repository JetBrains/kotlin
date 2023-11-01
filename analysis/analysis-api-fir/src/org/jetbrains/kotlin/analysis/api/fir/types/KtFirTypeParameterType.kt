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
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.name.Name

internal class KtFirTypeParameterType(
    override val coneType: ConeTypeParameterType,
    private val builder: KtSymbolByFirBuilder,
) : KtTypeParameterType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token
    override val name: Name get() = withValidityAssertion { coneType.lookupTag.name }
    override val symbol: KtTypeParameterSymbol by cached {
        builder.classifierBuilder.buildTypeParameterSymbolByLookupTag(coneType.lookupTag)
            ?: errorWithFirSpecificEntries("Type parameter was not found", coneType = coneType)
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
