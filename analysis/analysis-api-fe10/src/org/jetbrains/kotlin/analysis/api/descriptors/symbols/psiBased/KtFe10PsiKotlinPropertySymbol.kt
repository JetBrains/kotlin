/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtConstantValue
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtUnsupportedConstantValue
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
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

internal class KtFe10PsiKotlinPropertySymbol(
    override val psi: KtProperty,
    override val analysisContext: Fe10AnalysisContext
) : KtKotlinPropertySymbol(), KtFe10PsiSymbol<KtProperty, PropertyDescriptor> {
    override val descriptor: PropertyDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.VARIABLE, psi] as? PropertyDescriptor
    }

    override val isLateInit: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.LATEINIT_KEYWORD) }

    override val isConst: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.CONST_KEYWORD) }

    override val hasGetter: Boolean
        get() = withValidityAssertion { true }

    override val hasSetter: Boolean
        get() = withValidityAssertion { psi.isVar }

    override val getter: KtPropertyGetterSymbol
        get() = withValidityAssertion {
            val getter = psi.getter ?: return KtFe10PsiDefaultPropertyGetterSymbol(psi, analysisContext)
            return KtFe10PsiPropertyGetterSymbol(getter, analysisContext)
        }

    override val setter: KtPropertySetterSymbol?
        get() = withValidityAssertion {
            if (!psi.isVar) {
                return null
            }

            val setter = psi.setter ?: return KtFe10PsiDefaultPropertySetterSymbol(psi, analysisContext)
            return KtFe10PsiPropertySetterSymbol(setter, analysisContext)
        }

    override val hasBackingField: Boolean
        get() = withValidityAssertion {
            val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
            bindingContext[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == true
        }

    override val isDelegatedProperty: Boolean
        get() = withValidityAssertion {
            psi.hasDelegate()
        }

    override val isFromPrimaryConstructor: Boolean
        get() = withValidityAssertion { false }

    override val isOverride: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.OVERRIDE_KEYWORD) }

    override val isStatic: Boolean
        get() = withValidityAssertion { false }

    override val initializer: KtConstantValue?
        get() = withValidityAssertion {
            val initializer = psi.initializer ?: return null

            val compileTimeInitializer = descriptor?.compileTimeInitializer
            if (compileTimeInitializer != null) {
                return compileTimeInitializer.toKtConstantValue()
            }

            val bindingContext = analysisContext.analyze(initializer)
            val constantValue = ConstantExpressionEvaluator.getConstant(initializer, bindingContext)
            if (constantValue != null) {
                return constantValue.toConstantValue(descriptor?.type ?: TypeUtils.NO_EXPECTED_TYPE).toKtConstantValue()
            }

            return KtUnsupportedConstantValue
        }

    override val isVal: Boolean
        get() = withValidityAssertion { !psi.isVar }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion { psi.callableIdIfNonLocal }

    override val type: KtType
        get() = withValidityAssertion { descriptor?.type?.toKtType(analysisContext) ?: createErrorType() }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            return if (psi.isExtensionDeclaration()) {
                descriptor?.extensionReceiverParameter?.type?.toKtType(analysisContext) ?: createErrorType()
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

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion {
            psi.typeParameters.map { KtFe10PsiTypeParameterSymbol(it, analysisContext) }
        }

    override val modality: Modality
        get() = withValidityAssertion { psi.ktModality ?: descriptor?.ktModality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KtSymbolPointer<KtKotlinPropertySymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}
