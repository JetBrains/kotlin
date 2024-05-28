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
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirDynamicType(
    override val coneType: ConeDynamicType,
    private val builder: KaSymbolByFirBuilder,
) : KaDynamicType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token
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
    override fun createPointer(): KaTypePointer<KaDynamicType> = withValidityAssertion {
        return KaFirDynamicTypePointer()
    }
}

@KaAnalysisNonPublicApi
private class KaFirDynamicTypePointer : KaTypePointer<KaDynamicType> {
    override fun restore(session: KaSession): KaDynamicType = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = ConeDynamicType.create(session.useSiteSession)
        return KaFirDynamicType(coneType, session.firSymbolBuilder)
    }
}