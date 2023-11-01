/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtUsualClassType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.name.ClassId

internal class KtFirUsualClassType(
    override val coneType: ConeClassLikeTypeImpl,
    private val builder: KtSymbolByFirBuilder,
) : KtUsualClassType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token
    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }
    override val classSymbol: KtClassLikeSymbol
        get() = withValidityAssertion {
            builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
                ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
        }

    override val qualifiers by cached {
        UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
    }

    override val ownTypeArguments: List<KtTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder)
    }

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }
    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}

