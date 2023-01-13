/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KtCapturedType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.types.checker.NewCapturedType

internal class KtFe10NewCapturedType(
    override val fe10Type: NewCapturedType,
    override val analysisContext: Fe10AnalysisContext
) : KtCapturedType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging(analysisContext) }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val projection: KtTypeProjection
        get() = withValidityAssertion { fe10Type.constructor.projection.toKtTypeProjection(analysisContext) }
}