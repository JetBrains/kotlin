/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KaFe10DescKotlinPropertySymbol(
    override val descriptor: PropertyDescriptorImpl,
    override val analysisContext: Fe10AnalysisContext
) : KaKotlinPropertySymbol(), KaFe10DescSymbol<PropertyDescriptorImpl> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { descriptor.kaSymbolLocation }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { descriptor.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val isLateInit: Boolean
        get() = withValidityAssertion { descriptor.isLateInit }

    override val isConst: Boolean
        get() = withValidityAssertion { descriptor.isConst }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { psi is KtParameter }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isEnumEntry(descriptor) }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val hasGetter: Boolean
        get() = withValidityAssertion { !descriptor.isDynamic() }

    override val hasSetter: Boolean
        get() = withValidityAssertion { !descriptor.isDynamic() && !isVal }

    override val callableId: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val initializer: KaInitializerValue?
        get() = withValidityAssertion {
            val initializer = when (val psi = psi) {
                is KtProperty -> psi.initializer
                is KtParameter -> psi
                else -> null
            }

            createKtInitializerValue(initializer, descriptor, analysisContext)
        }

    override val getter: KaPropertyGetterSymbol?
        get() = withValidityAssertion {
            if (descriptor.isDynamic()) return null
            val getter = descriptor.getter ?: return KaFe10DescDefaultPropertyGetterSymbol(descriptor, analysisContext)
            return KaFe10DescPropertyGetterSymbol(getter, analysisContext)
        }

    override val setter: KaPropertySetterSymbol?
        get() = withValidityAssertion {
            if (descriptor.isDynamic()) return null
            if (!descriptor.isVar) {
                return null
            }

            val setter = descriptor.setter ?: return KaFe10DescDefaultPropertySetterSymbol(descriptor, analysisContext)
            return KaFe10DescPropertySetterSymbol(setter, analysisContext)
        }

    override val backingFieldSymbol: KaBackingFieldSymbol?
        get() = withValidityAssertion {
            if (descriptor.containingDeclaration is FunctionDescriptor) null
            else KaFe10DescDefaultBackingFieldSymbol(descriptor.backingField, this, analysisContext)
        }

    override val returnType: KaType
        get() = withValidityAssertion { descriptor.type.toKtType(analysisContext) }

    override val receiverParameter: KaReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { it.toKtTypeParameter(analysisContext) } }

    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            val bindingContext = analysisContext.resolveSession.bindingContext
            return bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
        }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { descriptor.isDelegated }

    override fun createPointer(): KaSymbolPointer<KaKotlinPropertySymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaKotlinPropertySymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
