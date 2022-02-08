/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KtFe10DescKotlinPropertySymbol(
    override val descriptor: PropertyDescriptorImpl,
    override val analysisContext: Fe10AnalysisContext
) : KtKotlinPropertySymbol(), KtFe10DescMemberSymbol<PropertyDescriptorImpl> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { descriptor.ktSymbolKind }

    override val isLateInit: Boolean
        get() = withValidityAssertion { descriptor.isLateInit }

    override val isConst: Boolean
        get() = withValidityAssertion { descriptor.isConst }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { descriptor.containingDeclaration is ConstructorDescriptor }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isEnumEntry(descriptor) }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val hasGetter: Boolean
        get() = withValidityAssertion { true }

    override val hasSetter: Boolean
        get() = withValidityAssertion { descriptor.isVar }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val initializer: KtInitializerValue?
        get() = withValidityAssertion { createKtInitializerValue(source as? KtProperty, descriptor, analysisContext) }

    override val getter: KtPropertyGetterSymbol
        get() = withValidityAssertion {
            val getter = descriptor.getter ?: return KtFe10DescDefaultPropertyGetterSymbol(descriptor, analysisContext)
            return KtFe10DescPropertyGetterSymbol(getter, analysisContext)
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            if (!descriptor.isVar) {
                return null
            }

            val setter = descriptor.setter ?: return KtFe10DescDefaultPropertySetterSymbol(descriptor, analysisContext)
            return KtFe10DescPropertySetterSymbol(setter, analysisContext)
        }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.type.toKtType(analysisContext) }

    override val receiverType: KtType?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.type?.toKtType(analysisContext) }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { it.toKtTypeParameter(analysisContext) } }

    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            val bindingContext = analysisContext.resolveSession.bindingContext
            return bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
        }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion { descriptor.delegateField != null }

    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}
