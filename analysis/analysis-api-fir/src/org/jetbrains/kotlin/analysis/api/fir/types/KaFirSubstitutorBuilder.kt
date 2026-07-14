/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.types.KaType

@OptIn(KaImplementationDetail::class)
internal class KaFirSubstitutorBuilder(override val token: KaLifetimeToken) : KaSubstitutorBuilder {
    private val backingMapping = mutableMapOf<KaTypeParameterSymbol, KaType>()

    override val mappings: Map<KaTypeParameterSymbol, KaType>
        get() = withValidityAssertion { backingMapping }

    override fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    override fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}
