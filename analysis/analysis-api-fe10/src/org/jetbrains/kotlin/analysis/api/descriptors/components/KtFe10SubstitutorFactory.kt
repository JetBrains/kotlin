/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtSubstitutorBuilder
import org.jetbrains.kotlin.analysis.api.components.KtSubstitutorFactory
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor

internal class KtFe10SubstitutorFactory(
    override val analysisSession: KtFe10AnalysisSession
) : KtSubstitutorFactory(), Fe10KtAnalysisSessionComponent {

    override fun buildSubstitutor(builder: KtSubstitutorBuilder): KtSubstitutor {
        if (builder.mappings.isEmpty()) return KtSubstitutor.Empty(token)
        TODO()
    }
}