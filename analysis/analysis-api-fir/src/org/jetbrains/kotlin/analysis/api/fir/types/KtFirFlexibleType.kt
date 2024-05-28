/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirFlexibleType(
    override val coneType: ConeFlexibleType,
    private val builder: KaSymbolByFirBuilder,
) : KaFlexibleType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val lowerBound: KaType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.lowerBound) }
    override val upperBound: KaType get() = withValidityAssertion { builder.typeBuilder.buildKtType(coneType.upperBound) }
    override val annotationsList: KaAnnotationsList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }
    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaAnalysisNonPublicApi
    override fun createPointer(): KaTypePointer<KaFlexibleType> = withValidityAssertion {
        return KaFirFlexibleTypePointer(
            lowerBoundPointer = lowerBound.createPointer(),
            upperBoundPointer = upperBound.createPointer()
        )
    }
}

@KaAnalysisNonPublicApi
private class KaFirFlexibleTypePointer(
    private val lowerBoundPointer: KaTypePointer<*>,
    private val upperBoundPointer: KaTypePointer<*>
) : KaTypePointer<KaFlexibleType> {
    override fun restore(session: KaSession): KaFlexibleType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val lowerBoundType = lowerBoundPointer.restore(session) as? KaFirType ?: return null
        val lowerBoundConeType = lowerBoundType.coneType as? ConeSimpleKotlinType ?: return null

        val upperBoundType = upperBoundPointer.restore(session) as? KaFirType ?: return null
        val upperBoundConeType = upperBoundType.coneType as? ConeSimpleKotlinType ?: return null

        val coneType = ConeFlexibleType(lowerBoundConeType, upperBoundConeType)
        return KaFirFlexibleType(coneType, session.firSymbolBuilder)
    }
}