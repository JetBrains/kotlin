/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiKotlinPropertySymbol(
    override val psi: KtProperty,
    override val analysisSession: KtFe10AnalysisSession
) : KtKotlinPropertySymbol(), KtFe10PsiSymbol<KtProperty, PropertyDescriptor> {
    override val descriptor: PropertyDescriptor? by cached {
        val bindingContext = analysisSession.analyze(psi, KtFe10AnalysisSession.AnalysisMode.PARTIAL)
        bindingContext[BindingContext.VARIABLE, psi] as? PropertyDescriptor
    }

    override val isLateInit: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.LATEINIT_KEYWORD) }

    override val isConst: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.CONST_KEYWORD) }

    override val hasGetter: Boolean
        get() = withValidityAssertion { psi.getter != null }

    override val hasSetter: Boolean
        get() = withValidityAssertion { psi.setter != null }

    override val getter: KtPropertyGetterSymbol?
        get() = withValidityAssertion {
            val getter = psi.getter ?: return null
            return KtFe10PsiPropertyGetterSymbol(getter, analysisSession)
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            val setter = psi.setter ?: return null
            return KtFe10PsiPropertySetterSymbol(setter, analysisSession)
        }

    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            val bindingContext = analysisSession.analyze(psi, KtFe10AnalysisSession.AnalysisMode.PARTIAL)
            bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val isStatic: Boolean
        get() = withValidityAssertion { false }

    override val initializer: KtConstantValue?
        get() = withValidityAssertion { descriptor?.compileTimeInitializer?.toKtConstantValue() }

    override val isVal: Boolean
        get() = withValidityAssertion { !psi.isVar }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { psi.callableId }

    override val annotatedType: KtTypeAndAnnotations
        get() = withValidityAssertion { descriptor?.type?.toKtTypeAndAnnotations(analysisSession) ?: createErrorTypeAndAnnotations() }

    override val receiverType: KtTypeAndAnnotations?
        get() = withValidityAssertion {
            return if (psi.isExtensionDeclaration()) {
                descriptor?.extensionReceiverParameter?.type?.toKtTypeAndAnnotations(analysisSession) ?: createErrorTypeAndAnnotations()
            } else {
                null
            }
        }

    override val isExtension: Boolean
        get() = withValidityAssertion { psi.isExtensionDeclaration() }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { psi.ktSymbolKind }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val dispatchType: KtType?
        get() = withValidityAssertion {
            return if (!isStatic) {
                descriptor?.dispatchReceiverParameter?.type?.toKtType(analysisSession) ?: createErrorType()
            } else {
                null
            }
        }

    override val modality: Modality
        get() = withValidityAssertion { psi.ktModality ?: descriptor?.modality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}