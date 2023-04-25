/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KtFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtTypeErrorType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.fir.diagnostics.ConeCannotInferTypeParameterType
import org.jetbrains.kotlin.fir.diagnostics.ConeTypeVariableTypeIsNotInferred
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KtFirTypeErrorType(
    override val coneType: ConeErrorType,
    private val builder: KtSymbolByFirBuilder,
) : KtTypeErrorType(), KtFirType {
    override val token: KtLifetimeToken get() = builder.token

    override val nullability: KtTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }
    override val errorMessage: String get() = withValidityAssertion { coneType.diagnostic.reason }

    override fun tryRenderAsNonErrorType(): String? = withValidityAssertion {
        when (val diagnostic = coneType.diagnostic) {
            is ConeCannotInferTypeParameterType -> diagnostic.typeParameter.name.asString()
            is ConeTypeVariableTypeIsNotInferred -> diagnostic.typeVariableType.lookupTag.debugName
            else -> null
        }
    }

    override val annotationsList: KtAnnotationsList by cached {
        KtFirAnnotationListForType.create(coneType, builder.rootSession, token)
    }

    override fun asStringForDebugging(): String = withValidityAssertion { coneType.renderForDebugging() }
    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
}
