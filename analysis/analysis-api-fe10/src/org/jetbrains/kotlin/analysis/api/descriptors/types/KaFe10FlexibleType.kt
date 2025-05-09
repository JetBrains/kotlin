/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.types.FlexibleType

internal class KaFe10FlexibleType(
    override val fe10Type: FlexibleType,
    override val analysisContext: Fe10AnalysisContext
) : KaFlexibleType(), KaFe10Type {
    override val lowerBound: KaType
        get() = withValidityAssertion { fe10Type.lowerBound.toKtType(analysisContext) }

    override val upperBound: KaType
        get() = withValidityAssertion { fe10Type.upperBound.toKtType(analysisContext) }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaFlexibleType> = withValidityAssertion {
        throw NotImplementedError("Type pointers are not implemented for FE 1.0")
    }
}
