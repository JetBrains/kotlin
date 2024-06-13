/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktSymbolKind
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.invalidEnumEntryAsClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiNamedClassOrObjectSymbol(
    override val psi: KtClassOrObject,
    override val analysisContext: Fe10AnalysisContext
) : KaNamedClassOrObjectSymbol(), KaFe10PsiSymbol<KtClassOrObject, ClassDescriptor> {
    override val descriptor: ClassDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.CLASS, psi]
    }

    override val isInner: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INNER_KEYWORD) }

    override val isData: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.DATA_KEYWORD) }

    override val isInline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INLINE_KEYWORD) || psi.hasModifier(KtTokens.VALUE_KEYWORD) }

    override val isFun: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.FUN_KEYWORD) }

    override val isExternal: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.EXTERNAL_KEYWORD) }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor?.isActual ?: psi.hasActualModifier() }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor?.isExpect ?: psi.hasExpectModifier() }

    override val companionObject: KaNamedClassOrObjectSymbol?
        get() = withValidityAssertion {
            val companionObject = psi.companionObjects.firstOrNull() ?: return null
            KaFe10PsiNamedClassOrObjectSymbol(companionObject, analysisContext)
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor?.createContextReceivers(analysisContext) ?: emptyList() }


    @OptIn(KaAnalysisApiInternals::class)
    override val classKind: KaClassKind
        get() = withValidityAssertion {
            when (psi) {
                is KtEnumEntry -> invalidEnumEntryAsClassKind()
                is KtObjectDeclaration -> when {
                    psi.isCompanion() -> KaClassKind.COMPANION_OBJECT
                    psi.isObjectLiteral() -> KaClassKind.ANONYMOUS_OBJECT
                    else -> KaClassKind.OBJECT
                }
                is KtClass -> when {
                    psi.isInterface() -> KaClassKind.INTERFACE
                    psi.isEnum() -> KaClassKind.ENUM_CLASS
                    psi.isAnnotation() -> KaClassKind.ANNOTATION_CLASS
                    else -> KaClassKind.CLASS
                }
                else -> error("Unexpected class instance")
            }
        }

    override val superTypes: List<KaType>
        get() = withValidityAssertion {
            descriptor?.getSupertypesWithAny()?.map { it.toKtType(analysisContext) } ?: emptyList()
        }

    override val classId: ClassId?
        get() = withValidityAssertion { psi.getClassId() }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val symbolKind: KaSymbolKind
        get() = withValidityAssertion { psi.ktSymbolKind }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KaFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val modality: Modality
        get() = withValidityAssertion { psi.ktModality ?: descriptor?.ktModality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KaSymbolPointer<KaNamedClassOrObjectSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaNamedClassOrObjectSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
