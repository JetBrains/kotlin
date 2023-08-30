/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtMultiplatformInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver

internal class KtFe10MultiplatformInfoProvider(
    override val analysisSession: KtFe10AnalysisSession,
) : KtMultiplatformInfoProvider(), Fe10KtAnalysisSessionComponent {
    override fun getExpectForActual(actual: KtDeclarationSymbol): List<KtDeclarationSymbol> {
        if (actual.psiSafe<KtDeclaration>()?.hasActualModifier() != true) return emptyList()
        val memberDescriptor = (getSymbolDescriptor(actual) as? MemberDescriptor)?.takeIf { it.isActual } ?: return emptyList()

        val expectedCompatibilityMap =
            ExpectedActualResolver.findExpectedForActual(memberDescriptor) ?: return emptyList()

        val expectsForActual = (expectedCompatibilityMap[ExpectActualCompatibility.Compatible]
            ?: expectedCompatibilityMap.values.flatten())
        return expectsForActual.map { it.toKtSymbol(analysisContext) as KtDeclarationSymbol }
    }
}