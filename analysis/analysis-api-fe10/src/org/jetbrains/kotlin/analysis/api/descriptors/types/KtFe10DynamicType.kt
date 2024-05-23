/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.types.DynamicType

internal class KaFe10DynamicType(
    override val fe10Type: DynamicType,
    override val analysisContext: Fe10AnalysisContext
) : KaDynamicType(), KaFe10Type {
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }
}
