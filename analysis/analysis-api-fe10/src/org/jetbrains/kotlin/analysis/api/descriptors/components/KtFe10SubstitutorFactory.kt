/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.components.KaSubstitutorFactory
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor

internal class KaFe10SubstitutorFactory(
    override val analysisSession: KaFe10Session
) : KaSubstitutorFactory(), KaFe10SessionComponent {

    override fun buildSubstitutor(builder: KaSubstitutorBuilder): KaSubstitutor {
        if (builder.mappings.isEmpty()) return KaSubstitutor.Empty(token)
        TODO()
    }
}