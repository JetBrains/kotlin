/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KtFe10AnnotationsList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10PsiDefaultBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations

internal class KtFe10DescDefaultBackingFieldSymbol(
    private val fieldDescriptor: FieldDescriptor?,
    override val owningProperty: KtKotlinPropertySymbol,
    override val analysisContext: Fe10AnalysisContext
) : KtBackingFieldSymbol(), KtFe10Symbol {
    context(KtAnalysisSession) override fun createPointer(): KtSymbolPointer<KtBackingFieldSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtPropertySymbol>(owningProperty)
            ?.let { KtFe10PsiDefaultBackingFieldSymbolPointer(it) }
            ?: KtFe10NeverRestoringSymbolPointer()
    }

    override val returnType: KtType
        get() = withValidityAssertion { owningProperty.returnType }

    override val token: KtLifetimeToken
        get() = owningProperty.token

    override val annotationsList: KtAnnotationsList
        get() = withValidityAssertion {
            KtFe10AnnotationsList.create(fieldDescriptor?.annotations ?: Annotations.EMPTY, analysisContext)
        }
}