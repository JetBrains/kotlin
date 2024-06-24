/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirErrorType(
    override val coneType: ConeErrorType,
    private val coneNullability: ConeNullability,
    private val builder: KaSymbolByFirBuilder,
) : KaErrorType, KaFirType {

    override val token: KaLifetimeToken get() = builder.token

    override val nullability: KaTypeNullability get() = withValidityAssertion { coneNullability.asKtNullability() }

    @KaNonPublicApi
    override val errorMessage: String
        get() = withValidityAssertion { coneType.diagnostic.reason }

    @KaNonPublicApi
    override val presentableText: String?
        get() = withValidityAssertion {
            when (val diagnostic = coneType.diagnostic) {
                is ConeCannotInferTypeParameterType -> diagnostic.typeParameter.name.asString()
                is ConeTypeVariableTypeIsNotInferred -> diagnostic.typeVariableType.typeConstructor.debugName
                else -> null
            }
        }

    override val annotations: KaAnnotationList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaErrorType> = withValidityAssertion {
        return KaFirErrorTypePointer(coneType, builder, coneNullability)
    }
}

private class KaFirErrorTypePointer(
    coneType: ConeErrorType,
    builder: KaSymbolByFirBuilder,
    private val coneNullability: ConeNullability
) : KaTypePointer<KaErrorType> {
    private val coneTypePointer = coneType.createPointer(builder)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaErrorType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        return KaFirErrorType(coneType, coneNullability, session.firSymbolBuilder)
    }
}