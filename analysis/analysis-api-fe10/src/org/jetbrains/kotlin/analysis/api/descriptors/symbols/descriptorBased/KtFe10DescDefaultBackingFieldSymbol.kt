/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10PsiDefaultBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations

internal class KaFe10DescDefaultBackingFieldSymbol(
    private val fieldDescriptor: FieldDescriptor?,
    override val owningProperty: KaKotlinPropertySymbol,
    override val analysisContext: Fe10AnalysisContext
) : KaBackingFieldSymbol(), KaFe10Symbol {
    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaPropertySymbol>(owningProperty)
            ?.let { KaFe10PsiDefaultBackingFieldSymbolPointer(it) }
            ?: KaFe10NeverRestoringSymbolPointer()
    }

    override val returnType: KaType
        get() = withValidityAssertion { owningProperty.returnType }

    override val token: KaLifetimeToken
        get() = owningProperty.token

    override val annotationsList: KaAnnotationsList
        get() = withValidityAssertion {
            KaFe10AnnotationsList.create(fieldDescriptor?.annotations ?: Annotations.EMPTY, analysisContext)
        }
}