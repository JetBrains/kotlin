/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.types.KtDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.types.DefinitelyNotNullType

internal class KtFe10DefinitelyNotNullType(
    override val fe10Type: DefinitelyNotNullType,
    override val analysisContext: Fe10AnalysisContext
) : KtDefinitelyNotNullType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging() }

    override val original: KtType
        get() = withValidityAssertion { fe10Type.original.toKtType(analysisContext) }
}