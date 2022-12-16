/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.types.KtFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.types.FlexibleType

internal class KtFe10FlexibleType(
    override val fe10Type: FlexibleType,
    override val analysisContext: Fe10AnalysisContext
) : KtFlexibleType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging() }

    override val lowerBound: KtType
        get() = withValidityAssertion { fe10Type.lowerBound.toKtType(analysisContext) }

    override val upperBound: KtType
        get() = withValidityAssertion { fe10Type.upperBound.toKtType(analysisContext) }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }
}