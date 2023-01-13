/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.SimpleType

internal class KtFe10TypeParameterType(
    override val fe10Type: SimpleType,
    private val parameter: TypeParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtTypeParameterType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging(analysisContext) }

    override val name: Name
        get() = withValidityAssertion { parameter.name }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val symbol: KtTypeParameterSymbol
        get() = withValidityAssertion { KtFe10DescTypeParameterSymbol(parameter, analysisContext) }
}