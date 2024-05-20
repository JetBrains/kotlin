/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KaFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.source.getPsi

internal class KaFe10DescValueParameterSymbol(
    override val descriptor: ValueParameterDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaValueParameterSymbol(), KaFe10DescSymbol<ValueParameterDescriptor> {
    override val name: Name
        get() = withValidityAssertion {
            return when (val name = descriptor.name) {
                SpecialNames.IMPLICIT_SET_PARAMETER -> Name.identifier("value")
                else -> name
            }
        }

    override val hasDefaultValue: Boolean
        get() = withValidityAssertion { descriptor.hasDefaultValue() }

    override val isVararg: Boolean
        get() = withValidityAssertion { descriptor.isVararg }

    override val isCrossinline: Boolean
        get() = withValidityAssertion { descriptor.isCrossinline }

    override val isNoinline: Boolean
        get() = withValidityAssertion { descriptor.isNoinline }

    override val isImplicitLambdaParameter: Boolean
        get() = withValidityAssertion {
            descriptor.containingDeclaration is AnonymousFunctionDescriptor &&
                    descriptor.name == StandardNames.IMPLICIT_LAMBDA_PARAMETER_NAME &&
                    // Implicit lambda parameter doesn't have a source PSI.
                    descriptor.source.getPsi() == null &&
                    // But, that could be the case for a declaration from Library. Double-check the slice in the binding context
                    (descriptor.containingDeclaration.source.getPsi() as? KtFunctionLiteral)?.let { parentLambda ->
                        analysisContext.analyze(parentLambda).get(BindingContext.AUTO_CREATED_IT, descriptor) != null
                    } == true
        }

    override val returnType: KaType
        get() = withValidityAssertion {
            return (descriptor.varargElementType ?: descriptor.type).toKtType(analysisContext)
        }

    override fun createPointer(): KaSymbolPointer<KaValueParameterSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaValueParameterSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()

}
