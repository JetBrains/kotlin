/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.kaSymbolLocation
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.kaSymbolModality
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.ktVisibility
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.invalidEnumEntryAsClassKind
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBasePsiSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
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

internal class KaFe10PsiNamedClassSymbol(
    override val psi: KtClassOrObject,
    override val analysisContext: Fe10AnalysisContext
) : KaNamedClassSymbol(), KaFe10PsiSymbol<KtClassOrObject, ClassDescriptor> {
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

    override val companionObject: KaNamedClassSymbol?
        get() = withValidityAssertion {
            val companionObject = psi.companionObjects.firstOrNull() ?: return null
            KaFe10PsiNamedClassSymbol(companionObject, analysisContext)
        }

    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor?.createContextReceivers(analysisContext) ?: emptyList() }


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
            descriptor?.computeSymbolSupertypes()?.map { it.toKtType(analysisContext) } ?: emptyList()
        }

    override val classId: ClassId?
        get() = withValidityAssertion { psi.getClassId() }

    override val name: Name
        get() = withValidityAssertion { psi.nameAsSafeName }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { psi.kaSymbolLocation }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { psi.typeParameters.map { KaFe10PsiTypeParameterSymbol(it, analysisContext) } }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { psi.kaSymbolModality ?: descriptor?.kaSymbolModality ?: KaSymbolModality.FINAL }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { psi.ktVisibility ?: descriptor?.ktVisibility ?: Visibilities.Public }

    override fun createPointer(): KaSymbolPointer<KaNamedClassSymbol> = withValidityAssertion {
        KaBasePsiSymbolPointer.createForSymbolFromSource<KaNamedClassSymbol>(this) ?: KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}
