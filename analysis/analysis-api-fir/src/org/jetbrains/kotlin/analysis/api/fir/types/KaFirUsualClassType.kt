/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
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

    override val qualifiers: List<KaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
        }

    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForType.create(coneType, builder)
        }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability get() = withValidityAssertion { KaTypeNullability.create(coneType.isMarkedNullable) }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion {
            builder.buildAbbreviatedType(coneType)
        }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaUsualClassType> = withValidityAssertion {
        return KaFirUsualClassTypePointer(coneType, builder)
    }
}

private class KaFirUsualClassTypePointer(
    coneType: ConeClassLikeTypeImpl,
    builder: KaSymbolByFirBuilder,
) : KaTypePointer<KaUsualClassType> {
    private val coneTypePointer = coneType.createPointer(builder)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaUsualClassType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        if (coneType.isSomeFunctionType(session.resolutionFacade.useSiteFirSession)) {
            return null
        }

        return KaFirUsualClassType(coneType, session.firSymbolBuilder)
    }
}