/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getSupertypesWithAny
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktSymbolKind
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10PsiNamedClassOrObjectSymbol(
    override val psi: KtClassOrObject,
    override val analysisContext: Fe10AnalysisContext
) : KtNamedClassOrObjectSymbol(), KtFe10PsiSymbol<KtClassOrObject, ClassDescriptor> {
    override val descriptor: ClassDescriptor? by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        bindingContext[BindingContext.CLASS, psi]
    }

    override val isInner: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INNER_KEYWORD) }

    override val isData: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.DATA_KEYWORD) }

    override val isInline: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.INLINE_KEYWORD) }

    override val isFun: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.FUN_KEYWORD) }

    override val isExternal: Boolean
        get() = withValidityAssertion { psi.hasModifier(KtTokens.EXTERNAL_KEYWORD) }

    override val companionObject: KtNamedClassOrObjectSymbol?
        get() = withValidityAssertion {
            val companionObject = psi.companionObjects.firstOrNull() ?: return null
            KtFe10PsiNamedClassOrObjectSymbol(companionObject, analysisContext)
        }

    override val classKind: KtClassKind
        get() = withValidityAssertion {
            when (psi) {
                is KtEnumEntry -> KtClassKind.ENUM_ENTRY
                is KtObjectDeclaration -> when {
                    psi.isCompanion() -> KtClassKind.COMPANION_OBJECT
                    psi.isObjectLiteral() -> KtClassKind.ANONYMOUS_OBJECT
                    else -> KtClassKind.OBJECT
                }
                is KtClass -> when {
                    psi.isInterface() -> KtClassKind.INTERFACE
                    psi.isEnum() -> KtClassKind.ENUM_CLASS
                    psi.isAnnotation() -> KtClassKind.ANNOTATION_CLASS
                    else -> KtClassKind.CLASS
                }
                else -> error("Unexpected class instance")
            }
        }

    override val superTypes: List<KtType>
        get() = withValidityAssertion {
            descriptor?.getSupertypesWithAny()?.map { it.toKtType(analysisContext) } ?: emptyList()
        }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { psi.getClassId() }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { psi.ktSymbolKind }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KtFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val modality: Modality
        get() = withValidityAssertion { psi.ktModality ?: descriptor?.ktModality ?: Modality.FINAL }

    override val visibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        return KtPsiBasedSymbolPointer.createForSymbolFromSource(this) ?: KtFe10NeverRestoringSymbolPointer()
    }
}