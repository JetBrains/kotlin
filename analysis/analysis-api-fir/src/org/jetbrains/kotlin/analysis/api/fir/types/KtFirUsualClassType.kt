/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeClassLikeTypePointer
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.name.ClassId

internal class KaFirUsualClassType(
    override val coneType: ConeClassLikeTypeImpl,
    private val builder: KaSymbolByFirBuilder,
) : KaUsualClassType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }

    override val symbol: KaClassLikeSymbol
        get() = withValidityAssertion {
            builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
                ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
        }

    override val qualifiers by cached {
        UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
    }

    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val annotationsList: KaAnnotationsList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val abbreviatedType: KaUsualClassType? by cached {
        builder.buildAbbreviatedType(coneType)
    }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaAnalysisNonPublicApi
    override fun createPointer(): KaTypePointer<KaNonErrorClassType> = withValidityAssertion {
        val coneTypePointer = ConeClassLikeTypePointer(symbol, coneType, builder)
        return KaFirUsualClassTypePointer(coneTypePointer)
    }
}

@KaAnalysisNonPublicApi
private class KaFirUsualClassTypePointer(private val coneTypePointer: ConeClassLikeTypePointer) : KaTypePointer<KaUsualClassType> {
    override fun restore(session: KaSession): KaUsualClassType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        if (coneType.isSomeFunctionType(session.firResolveSession.useSiteFirSession)) {
            return null
        }

        return KaFirUsualClassType(coneType, session.firSymbolBuilder)
    }
}