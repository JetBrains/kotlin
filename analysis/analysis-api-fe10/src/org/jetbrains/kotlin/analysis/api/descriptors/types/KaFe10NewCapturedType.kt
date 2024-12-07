/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.types.checker.NewCapturedType

internal class KaFe10NewCapturedType(
    override val fe10Type: NewCapturedType,
    override val analysisContext: Fe10AnalysisContext
) : KaCapturedType(), KaFe10Type {
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val projection: KaTypeProjection
        get() = withValidityAssertion { fe10Type.constructor.projection.toKtTypeProjection(analysisContext) }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaCapturedType> = withValidityAssertion {
        throw NotImplementedError("Type pointers are not implemented for FE 1.0")
    }
}
