/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescNamedClassOrObjectSymbolSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class KtFe10DescNamedClassOrObjectSymbol(
    override val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtNamedClassOrObjectSymbol(), KtFe10DescMemberSymbol<ClassDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isInner: Boolean
        get() = withValidityAssertion { descriptor.isInner }

    override val isData: Boolean
        get() = withValidityAssertion { descriptor.isData }

    override val isInline: Boolean
        get() = withValidityAssertion { descriptor.isInline }

    override val isFun: Boolean
        get() = withValidityAssertion { descriptor.isFun }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val companionObject: KtNamedClassOrObjectSymbol?
        get() {
            withValidityAssertion {
                val companionObject = descriptor.companionObjectDescriptor ?: return null
                return KtFe10DescNamedClassOrObjectSymbol(companionObject, analysisContext)
            }
        }

    override val classKind: KtClassKind
        get() = withValidityAssertion {
            if (descriptor.isCompanionObject) {
                return KtClassKind.COMPANION_OBJECT
            } else if (DescriptorUtils.isAnonymousObject(descriptor)) {
                error("Should be an anonymous object")
            }

            return when (descriptor.kind) {
                ClassKind.CLASS -> KtClassKind.CLASS
                ClassKind.INTERFACE -> KtClassKind.INTERFACE
                ClassKind.ENUM_CLASS -> KtClassKind.ENUM_CLASS
                ClassKind.ENUM_ENTRY -> KtClassKind.ENUM_ENTRY
                ClassKind.ANNOTATION_CLASS -> KtClassKind.ANNOTATION_CLASS
                ClassKind.OBJECT -> KtClassKind.OBJECT
            }
        }

    override val superTypes: List<KtType>
        get() = withValidityAssertion {
            descriptor.getSupertypesWithAny().map { it.toKtType(analysisContext) }
        }

    override val classIdIfNonLocal: ClassId?
        get() = withValidityAssertion { descriptor.classId }

    override val symbolKind: KtSymbolKind
        get() = withValidityAssertion { descriptor.ktSymbolKind }

    override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.declaredTypeParameters.map { KtFe10DescTypeParameterSymbol(it, analysisContext) } }

    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        val pointerByPsi = KtPsiBasedSymbolPointer.createForSymbolFromSource(this)
        if (pointerByPsi != null) {
            return pointerByPsi
        }

        val classId = descriptor.classId
        if (classId != null) {
            return KtFe10DescNamedClassOrObjectSymbolSymbol(classId)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }
}