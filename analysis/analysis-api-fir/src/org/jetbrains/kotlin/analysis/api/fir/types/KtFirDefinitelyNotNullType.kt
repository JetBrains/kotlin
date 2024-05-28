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
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirDefinitelyNotNullType(
    override val coneType: ConeDefinitelyNotNullType,
    private val builder: KaSymbolByFirBuilder,
) : KaDefinitelyNotNullType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token
    override val original: KaType = withValidityAssertion { builder.typeBuilder.buildKtType(this.coneType.original) }
    override val annotationsList: KaAnnotationsList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaAnalysisNonPublicApi
    override fun createPointer(): KaTypePointer<KaDefinitelyNotNullType> = withValidityAssertion {
        return KaFirDefinitelyNotNullTypePointer(basePointer = original.createPointer())
    }
}

@KaAnalysisNonPublicApi
private class KaFirDefinitelyNotNullTypePointer(private val basePointer: KaTypePointer<*>) : KaTypePointer<KaDefinitelyNotNullType> {
    override fun restore(session: KaSession): KaDefinitelyNotNullType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val baseType = basePointer.restore(session) as? KaFirType ?: return null
        val baseConeType = baseType.coneType as ConeSimpleKotlinType

        val coneType = ConeDefinitelyNotNullType(baseConeType)
        return KaFirDefinitelyNotNullType(coneType, session.firSymbolBuilder)
    }
}