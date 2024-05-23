/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.getAbbreviation

internal class KaFe10ClassErrorType(
    override val fe10Type: ErrorType,
    override val analysisContext: Fe10AnalysisContext
) : KaClassErrorType(), KaFe10Type {
    init {
        check(fe10Type.kind.isUnresolved) {
            "Expected unresolved ErrorType but ${fe10Type.kind} found for $fe10Type"
        }
    }

    override val qualifiers: List<KaClassTypeQualifier.KaUnresolvedClassTypeQualifier>
        get() = withValidityAssertion {
            fe10Type.formatParams.first().split('.').map {
                KaClassTypeQualifier.KaUnresolvedClassTypeQualifier(Name.guessByFirstCharacter(it), emptyList(), token)
            }
        }

    @KaAnalysisNonPublicApi
    override val presentableText: String?
        get() = withValidityAssertion { fe10Type.formatParams.first() }

    @KaAnalysisNonPublicApi
    override val errorMessage: String
        get() = withValidityAssertion { fe10Type.debugMessage }

    override val candidateSymbols: Collection<KaClassLikeSymbol>
        get() = withValidityAssertion { emptyList() }

    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { fe10Type.getAbbreviation()?.toKtType(analysisContext) as? KaUsualClassType }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }
}
