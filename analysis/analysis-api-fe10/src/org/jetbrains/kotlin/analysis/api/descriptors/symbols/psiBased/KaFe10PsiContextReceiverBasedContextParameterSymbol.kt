/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiContextReceiverBasedContextParameterSymbol(
    override val psi: KtContextReceiver,
    override val analysisContext: Fe10AnalysisContext,
) : KaContextParameterSymbol(), KaFe10PsiSymbol<KtContextReceiver, ReceiverParameterDescriptor> {
    init {
        requireNotNull(psi.ownerDeclaration)
    }

    override val descriptor: ReceiverParameterDescriptor? by cached {
        val list = psi.parent as KtContextReceiverList
        val declaration = list.ownerDeclaration!!
        val bindingContext = analysisContext.analyze(declaration, AnalysisMode.PARTIAL)
        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? CallableDescriptor
        val index = list.contextReceivers().indexOf(psi)
        descriptor?.contextReceiverParameters?.getOrNull(index)
    }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { Visibilities.Public }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType() }

    override val name: Name
        get() = withValidityAssertion { psi.name()?.let(Name::identifier) ?: SpecialNames.UNDERSCORE_FOR_UNUSED_VAR }

    override fun createPointer(): KaSymbolPointer<KaContextParameterSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromSource<KaContextParameterSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
