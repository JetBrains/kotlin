/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.annotations.KaFe10AnnotationList
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10PsiDefaultBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.FieldDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiDefaultBackingFieldSymbol(
    private val propertyPsi: KtProperty,
    override val owningProperty: KaKotlinPropertySymbol,
    override val analysisContext: Fe10AnalysisContext
) : KaBackingFieldSymbol(), KaFe10Symbol {
    val descriptor: FieldDescriptor? by cached {
        val bindingContext = analysisContext.analyze(propertyPsi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        (bindingContext[BindingContext.VARIABLE, propertyPsi] as? PropertyDescriptor)?.backingField
    }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromPsi<KaPropertySymbol>(propertyPsi)
            ?.let(::KaFe10PsiDefaultBackingFieldSymbolPointer)
            ?: KaFe10NeverRestoringSymbolPointer()
    }

    override val returnType: KaType
        get() = withValidityAssertion { owningProperty.returnType }

    override val token: KaLifetimeToken
        get() = owningProperty.token

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFe10AnnotationList.create(descriptor?.annotations ?: Annotations.EMPTY, analysisContext)
        }
}
