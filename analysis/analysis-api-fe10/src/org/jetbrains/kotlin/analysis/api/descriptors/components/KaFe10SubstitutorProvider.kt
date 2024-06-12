/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.NotSupportedForK1Exception
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType

internal class KaFe10SubstitutorProvider(
    override val analysisSessionProvider: () -> KaSession
) : KaSessionComponent<KaSession>(), KaSubstitutorProvider {
    override fun createInheritanceTypeSubstitutor(subClass: KaClassOrObjectSymbol, superClass: KaClassOrObjectSymbol): KaSubstitutor? {
        withValidityAssertion {
            throw NotSupportedForK1Exception()
        }
    }

    override fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor = withValidityAssertion {
        if (mappings.isEmpty()) return KaSubstitutor.Empty(token)
        TODO()
    }
}