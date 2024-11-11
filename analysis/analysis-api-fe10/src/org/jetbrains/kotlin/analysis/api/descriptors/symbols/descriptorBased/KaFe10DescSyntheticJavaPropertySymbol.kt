/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

internal class KaFe10DescSyntheticJavaPropertySymbol(
    override val descriptor: SyntheticJavaPropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaSyntheticJavaPropertySymbol(), KaFe10DescSymbol<SyntheticJavaPropertyDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { descriptor.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val origin: KaSymbolOrigin
        get() = super<KaSyntheticJavaPropertySymbol>.origin

    override val getter: KaPropertyGetterSymbol
        get() = withValidityAssertion {
            val getter = descriptor.getter ?: return KaFe10DescDefaultPropertyGetterSymbol(descriptor, analysisContext)
            return KaFe10DescPropertyGetterSymbol(getter, analysisContext)
        }

    override val javaGetterSymbol: KaNamedFunctionSymbol
        get() = withValidityAssertion { KaFe10DescNamedFunctionSymbol.build(descriptor.getMethod, analysisContext) }

    override val javaSetterSymbol: KaNamedFunctionSymbol?
        get() = withValidityAssertion {
            val setMethod = descriptor.setMethod ?: return null
            return KaFe10DescNamedFunctionSymbol.build(setMethod, analysisContext)
        }

    override val hasSetter: Boolean
        get() = withValidityAssertion { descriptor.setter != null }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            if (!descriptor.isVar) {
                return null
            }

            val setter = descriptor.setter ?: return KaFe10DescDefaultPropertySetterSymbol(descriptor, analysisContext)
            KaFe10DescPropertySetterSymbol(setter, analysisContext)
        }

    override val initializer: KaInitializerValue?
        get() = withValidityAssertion { createKtInitializerValue((psi as? KtProperty)?.initializer, descriptor, analysisContext) }

    override val callableId: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.type.toKtType(analysisContext) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }


    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { it.toKtTypeParameter(analysisContext) } }

    override fun createPointer(): KaSymbolPointer<KaSyntheticJavaPropertySymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaSyntheticJavaPropertySymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}