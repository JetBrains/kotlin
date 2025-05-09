/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseUnresolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
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

    override val qualifiers: List<KaUnresolvedClassTypeQualifier>
        get() = withValidityAssertion {
            fe10Type.formatParams.first().split('.').map {
                KaBaseUnresolvedClassTypeQualifier(Name.guessByFirstCharacter(it), emptyList(), token)
            }
        }

    @KaNonPublicApi
    override val presentableText: String?
        get() = withValidityAssertion { fe10Type.formatParams.first() }

    @KaNonPublicApi
    override val errorMessage: String
        get() = withValidityAssertion { fe10Type.debugMessage }

    override val candidateSymbols: Collection<KaClassLikeSymbol>
        get() = withValidityAssertion { emptyList() }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { fe10Type.getAbbreviation()?.toKtType(analysisContext) as? KaUsualClassType }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaClassErrorType> = withValidityAssertion {
        throw NotImplementedError("Type pointers are not implemented for FE 1.0")
    }
}
