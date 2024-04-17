/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiValueParameterSymbol(
    override val psi: KtParameter,
    override val analysisContext: Fe10AnalysisContext
) : KtValueParameterSymbol(), KtFe10PsiSymbol<KtParameter, VariableDescriptor> {
    override val descriptor: VariableDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.VALUE_PARAMETER, psi]
    }

    override val generatedPrimaryConstructorProperty: KtKotlinPropertySymbol? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        val propertyDescriptor = bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, psi] ?: return@cached null

        KtFe10DescKotlinPropertySymbol(propertyDescriptor as PropertyDescriptorImpl, analysisContext)
    }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { psi.hasDefaultValue() }

    override val isVararg: Boolean
        get() = withValidityAssertion { psi.isVarArg }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.CROSSINLINE_KEYWORD) }

    override val isNoinline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.NOINLINE_KEYWORD) }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion { false }

    override val returnType: KtType
        get() = withValidityAssertion {
            val type = (descriptor as? ValueParameterDescriptor)?.varargElementType ?: descriptor?.type
            return type?.toKtType(analysisContext) ?: createErrorType()
        }

    override val name: Name
        get() = withValidityAssertion {
            if (psi.destructuringDeclaration != null) SpecialNames.DESTRUCT
            else psi.nameAsSafeName
        }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtValueParameterSymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
