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
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirCapturedType(
    override val coneType: ConeCapturedType,
    private val builder: KaSymbolByFirBuilder,
) : KaCapturedType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token
    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val projection: KaTypeProjection
        get() = withValidityAssertion { builder.typeBuilder.buildTypeProjection(coneType.constructor.projection) }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    @KaImplementationDetail
    override fun createPointer(): KaTypePointer<KaCapturedType> = withValidityAssertion {
        return KaFirCapturedTypePointer(coneType, builder)
    }
}

private class KaFirCapturedTypePointer(
    coneType: ConeCapturedType,
    builder: KaSymbolByFirBuilder
) : KaTypePointer<KaCapturedType> {
    private val coneTypePointer = coneType.createPointer(builder)

    override fun restore(session: KaSession): KaCapturedType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) as? ConeCapturedType ?: return null
        return KaFirCapturedType(coneType, session.firSymbolBuilder)
    }
}