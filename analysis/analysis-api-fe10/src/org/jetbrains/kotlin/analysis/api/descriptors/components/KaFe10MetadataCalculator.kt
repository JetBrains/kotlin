/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.google.common.collect.Multimap
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KaMetadataCalculator
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@OptIn(KaAnalysisNonPublicApi::class)
internal class KaFe10MetadataCalculator(
    override val analysisSessionProvider: () -> KaFe10Session,
    override val token: KaLifetimeToken
) : KaSessionComponent<KaFe10Session>(), KaMetadataCalculator {
    @KaAnalysisNonPublicApi
    override fun KtFile.calculateMetadata(mapping: Multimap<KtElement, PsiElement>): Metadata = withValidityAssertion {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }

    @KaAnalysisNonPublicApi
    override fun KtClassOrObject.calculateMetadata(mapping: Multimap<KtElement, PsiElement>): Metadata = withValidityAssertion {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}