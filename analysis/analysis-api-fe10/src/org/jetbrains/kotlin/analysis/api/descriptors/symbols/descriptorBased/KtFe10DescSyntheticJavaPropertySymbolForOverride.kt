/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KtFe10DescSyntheticJavaPropertySymbolForOverride(
    override val descriptor: JavaForKotlinOverridePropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtSyntheticJavaPropertySymbol(), KtFe10DescMemberSymbol<JavaForKotlinOverridePropertyDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { descriptor.containingDeclaration is ConstructorDescriptor }

    override val isOverride: Boolean
        get() = withValidityAssertion { descriptor.isExplicitOverride }

    override val isStatic: Boolean
        get() = withValidityAssertion { DescriptorUtils.isStaticDeclaration(descriptor) }

    override val isVal: Boolean
        get() = withValidityAssertion { !descriptor.isVar }

    override val isExtension: Boolean
        get() = withValidityAssertion { descriptor.isExtension }

    override val getter: KtPropertyGetterSymbol
        get() = withValidityAssertion {
            val getter = descriptor.getter ?: return KtFe10DescDefaultPropertyGetterSymbol(descriptor, analysisContext)
            return KtFe10DescPropertyGetterSymbol(getter, analysisContext)
        }

    override val javaGetterSymbol: KtFunctionSymbol
        get() = withValidityAssertion { KtFe10DescFunctionSymbol.build(descriptor.getterMethod, analysisContext) }

    override val javaSetterSymbol: KtFunctionSymbol?
        get() = withValidityAssertion {
            val setMethod = descriptor.setterMethod ?: return null
            return KtFe10DescFunctionSymbol.build(setMethod, analysisContext)
        }

    override val hasSetter: Boolean
        get() = withValidityAssertion { descriptor.setter != null }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            if (!descriptor.isVar) {
                return null
            }

            val setter = descriptor.setter ?: return KtFe10DescDefaultPropertySetterSymbol(descriptor, analysisContext)
            KtFe10DescPropertySetterSymbol(setter, analysisContext)
        }

    override val backingFieldSymbol: KtBackingFieldSymbol?
        get() = withValidityAssertion { null }

    override val initializer: KtInitializerValue?
        get() = withValidityAssertion { createKtInitializerValue((psi as? KtProperty)?.initializer, descriptor, analysisContext) }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableIdIfNotLocal }

    override val returnType: KtType
        get() = withValidityAssertion { descriptor.type.toKtType(analysisContext) }

    override val receiverParameter: KtReceiverParameterSymbol?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.toKtReceiverParameterSymbol(analysisContext) }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.typeParameters.map { it.toKtTypeParameter(analysisContext) } }

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtSyntheticJavaPropertySymbol>(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}