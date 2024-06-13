/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KaMultiplatformInfoProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.resolve.multiplatform.isCompatibleOrWeaklyIncompatible

internal class KaFe10MultiplatformInfoProvider(
    override val analysisSession: KaFe10Session,
) : KaMultiplatformInfoProvider(), KaFe10SessionComponent {
    override fun getExpectForActual(actual: KaDeclarationSymbol): List<KaDeclarationSymbol> {
        if (actual.psiSafe<KtDeclaration>()?.hasActualModifier() != true) return emptyList()
        val memberDescriptor = (getSymbolDescriptor(actual) as? MemberDescriptor)?.takeIf { it.isActual } ?: return emptyList()

        return ExpectedActualResolver.findExpectedForActual(memberDescriptor).orEmpty().asSequence()
            .filter { it.key.isCompatibleOrWeaklyIncompatible }
            .flatMap { it.value }
            .map { it.toKtSymbol(analysisContext) as KaDeclarationSymbol }
            .toList()
    }
}