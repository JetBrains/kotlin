/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescSyntheticFieldSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSymbolDescriptor
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor

class KaFe10DescSyntheticFieldSymbolPointer(
    private val psiPointer: KaPsiBasedSymbolPointer<KaPropertyAccessorSymbol>
) : KaSymbolPointer<KaBackingFieldSymbol>() {
    @KaImplementationDetail
    override fun restoreSymbol(analysisSession: KaSession): KaBackingFieldSymbol? {
        check(analysisSession is KaFe10Session)
        val analysisContext = analysisSession.analysisContext
        val accessorSymbol = psiPointer.restoreSymbol(analysisSession) ?: return null

        val accessorDescriptor = getSymbolDescriptor(accessorSymbol) as? PropertyAccessorDescriptor ?: return null
        val syntheticFieldDescriptor = SyntheticFieldDescriptor(accessorDescriptor, accessorDescriptor.correspondingProperty.source)
        return KaFe10DescSyntheticFieldSymbol(syntheticFieldDescriptor, analysisContext)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean = this === other ||
            other is KaFe10DescSyntheticFieldSymbolPointer &&
            other.psiPointer.pointsToTheSameSymbolAs(psiPointer)
}
