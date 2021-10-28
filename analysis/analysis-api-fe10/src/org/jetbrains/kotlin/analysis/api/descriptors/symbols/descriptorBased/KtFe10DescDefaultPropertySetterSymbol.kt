/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class KtFe10DescDefaultPropertySetterSymbol(
    private val propertyDescriptor: PropertyDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtPropertySetterSymbol(), KtFe10Symbol {
    override val parameter: KtValueParameterSymbol by cached {
        DefaultKtValueParameterSymbol(propertyDescriptor, analysisContext)
    }

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion { propertyDescriptor.isExplicitOverride }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { propertyDescriptor.setterCallableIdIfNotLocal }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion { analysisContext.builtIns.unitType.toKtTypeAndAnnotations(analysisContext) }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { propertyDescriptor.getSymbolOrigin(analysisContext) }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion { propertyDescriptor.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisContext) }

    override val dispatchType: KtType?
        get() = withValidityAssertion { propertyDescriptor.dispatchReceiverParameter?.type?.toKtType(analysisContext) }

    override val modality: Modality
        get() = withValidityAssertion { propertyDescriptor.modality }

    override val visibility: Visibility
        get() = withValidityAssertion { propertyDescriptor.ktVisibility }

    override val annotations: List<KtAnnotationCall>
        get() = withValidityAssertion { emptyList() }

    override fun containsAnnotation(classId: ClassId): Boolean {
        withValidityAssertion {
            return false
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { emptyList() }

    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> {
        withValidityAssertion {
            return KtFe10NeverRestoringSymbolPointer()
        }
    }

    private class DefaultKtValueParameterSymbol(
        private val propertyDescriptor: PropertyDescriptor,
        override val analysisContext: Fe10AnalysisContext
    ) : KtValueParameterSymbol(), KtFe10Symbol {
        override val hasDefaultValue: Boolean
            get() = withValidityAssertion { false }

        override val isVararg: Boolean
            get() = withValidityAssertion { false }

        override val name: Name
            get() = withValidityAssertion { Name.identifier("value") }

        override val annotatedType: KtTypeAndAnnotations
            get() = withValidityAssertion { propertyDescriptor.type.toKtTypeAndAnnotations(analysisContext) }

        override val origin: KtSymbolOrigin
            get() = withValidityAssertion { propertyDescriptor.getSymbolOrigin(analysisContext) }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val annotations: List<KtAnnotationCall>
            get() = withValidityAssertion { emptyList() }

        override fun containsAnnotation(classId: ClassId): Boolean {
            withValidityAssertion {
                return false
            }
        }

        override val annotationClassIds: Collection<ClassId>
            get() = withValidityAssertion { emptyList() }

        override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> {
            withValidityAssertion {
                return KtFe10NeverRestoringSymbolPointer()
            }
        }
    }
}