/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension

internal class KtFe10DescSyntheticJavaPropertySymbolForOverride(
    override val descriptor: JavaForKotlinOverridePropertyDescriptor,
    override val analysisSession: KtFe10AnalysisSession
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
            val getter = descriptor.getter ?: return EmptyGetterSymbol(descriptor, analysisSession)
            return KtFe10DescPropertyGetterSymbol(getter, analysisSession)
        }
    override val javaGetterSymbol: KtFunctionSymbol
        get() = withValidityAssertion { KtFe10DescFunctionSymbol(descriptor.getterMethod, analysisSession) }

    override val javaSetterSymbol: KtFunctionSymbol?
        get() = withValidityAssertion {
            val setMethod = descriptor.setterMethod ?: return null
            return KtFe10DescFunctionSymbol(setMethod, analysisSession)
        }

    override val hasSetter: Boolean
        get() = withValidityAssertion { descriptor.setter != null }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            val setter = descriptor.setter ?: return null
            KtFe10DescPropertySetterSymbol(setter, analysisSession)
        }

    override val initializer: KtConstantValue?
        get() = withValidityAssertion { descriptor.compileTimeInitializer?.toKtConstantValue() }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { descriptor.callableId }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion { descriptor.type.toKtTypeAndAnnotations(analysisSession) }

    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion { descriptor.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisSession) }

    override val dispatchType: KtType?
        get() = withValidityAssertion { descriptor.dispatchReceiverParameter?.type?.toKtType(analysisSession) }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY }

    override fun createPointer(): KtSymbolPointer<KtSyntheticJavaPropertySymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }

    private class EmptyGetterSymbol(
        private val descriptor: JavaForKotlinOverridePropertyDescriptor,
        override val analysisSession: KtFe10AnalysisSession
    ) : KtPropertyGetterSymbol(), KtFe10Symbol {
        override val isDefault: Boolean
            get() = withValidityAssertion { false }

        override val isInline: Boolean
            get() = withValidityAssertion { false }

        override val isOverride: Boolean
            get() = withValidityAssertion { descriptor.isExplicitOverride }

        override val hasBody: Boolean
            get() = withValidityAssertion { false }

        override val valueParameters: List<KtValueParameterSymbol>
            get() = withValidityAssertion { emptyList() }

        override val hasStableParameterNames: Boolean
            get() = withValidityAssertion { true }

        override val callableIdIfNonLocal: CallableId?
            get() = withValidityAssertion { null }

        override val annotatedType: KtTypeAndAnnotations
            get() = withValidityAssertion { descriptor.type.toKtTypeAndAnnotations(analysisSession) }

        override val origin: KtSymbolOrigin
            get() = withValidityAssertion { KtSymbolOrigin.JAVA }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val receiverType: KtTypeAndAnnotations?
            get() = withValidityAssertion { descriptor.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisSession) }

        override val dispatchType: KtType?
            get() = withValidityAssertion { descriptor.dispatchReceiverParameter?.type?.toKtType(analysisSession) }

        override val modality: Modality
            get() = withValidityAssertion { Modality.FINAL }

        override val visibility: Visibility
            get() = withValidityAssertion { descriptor.ktVisibility }

        override val annotations: List<KtAnnotationCall>
            get() = withValidityAssertion { emptyList() }

        override fun containsAnnotation(classId: ClassId): Boolean {
            withValidityAssertion {
                return false
            }
        }

        override val annotationClassIds: Collection<ClassId>
            get() = withValidityAssertion { emptyList() }

        override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol> {
            withValidityAssertion {
                return KtFe10NeverRestoringSymbolPointer()
            }
        }
    }
}