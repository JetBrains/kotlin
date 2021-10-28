/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

class KtFe10PsiDefaultPropertySetterSymbol(
    private val propertyPsi: KtProperty,
    override val analysisContext: Fe10AnalysisContext
) : KtPropertySetterSymbol(), KtFe10Symbol {
    private val descriptor: PropertyDescriptor? by cached {
        val bindingContext = analysisContext.analyze(propertyPsi, Fe10AnalysisFacade.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.VARIABLE, psi] as? PropertyDescriptor
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { propertyPsi.ktSymbolOrigin }

    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val isDefault: Boolean
        get() = withValidityAssertion { true }

    override val isInline: Boolean
        get() = withValidityAssertion { propertyPsi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val isOverride: Boolean
        get() = withValidityAssertion { propertyPsi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val hasBody: Boolean
        get() = withValidityAssertion { false }

    override val parameter: KtValueParameterSymbol by cached {
        DefaultKtValueParameterSymbol(propertyPsi, descriptor, analysisContext)
    }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { null }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion {
            val builtIns = analysisContext.resolveSession.moduleDescriptor.builtIns
            return builtIns.unitType.toKtTypeAndAnnotations(analysisContext)
        }

    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion {
            return if (propertyPsi.isExtensionDeclaration()) {
                descriptor?.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisContext) ?: createErrorTypeAndAnnotations()
            } else {
                null
            }
        }

    override val dispatchType: KtType?
        get() = withValidityAssertion {
            if (propertyPsi.isTopLevel) {
                return null
            }

            return descriptor?.dispatchReceiverParameter?.type?.toKtType(analysisContext) ?: createErrorType()
        }

    override val modality: Modality
        get() = withValidityAssertion { propertyPsi.ktModality ?: descriptor?.modality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { propertyPsi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override val annotations: List<KtAnnotationCall>
        get() = withValidityAssertion { emptyList() }

    override fun containsAnnotation(classId: ClassId): Boolean {
        withValidityAssertion {
            return false
        }
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { emptyList() }

    override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol> = withValidityAssertion {
        return KtFe10NeverRestoringSymbolPointer()
    }

    private class DefaultKtValueParameterSymbol(
        private val propertyPsi: KtProperty,
        private val descriptor: PropertyDescriptor?,
        override val analysisContext: Fe10AnalysisContext
    ) : KtValueParameterSymbol(), KtFe10Symbol {
        override val hasDefaultValue: Boolean
            get() = withValidityAssertion { false }

        override val isVararg: Boolean
            get() = withValidityAssertion { false }

        override val name: Name
            get() = withValidityAssertion { Name.identifier("value") }

        override val annotatedType: KtTypeAndAnnotations
            get() = withValidityAssertion { descriptor?.type?.toKtTypeAndAnnotations(analysisContext) ?: createErrorTypeAndAnnotations() }

        override val origin: KtSymbolOrigin
            get() = withValidityAssertion { propertyPsi.ktSymbolOrigin }

        override val psi: PsiElement?
            get() = withValidityAssertion { null }

        override val annotations: List<KtAnnotationCall>
            get() = withValidityAssertion { emptyList() }

        override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion {
            return false
        }

        override val annotationClassIds: Collection<ClassId>
            get() = withValidityAssertion { emptyList() }

        override fun createPointer(): KtSymbolPointer<KtValueParameterSymbol> = withValidityAssertion {
            return KtFe10NeverRestoringSymbolPointer()
        }
    }
}