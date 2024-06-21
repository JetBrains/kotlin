/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescNamedClassSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal class KaFe10DescNamedClassSymbol(
    override val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaNamedClassSymbol(), KaFe10DescMemberSymbol<ClassDescriptor> {
    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    override val isInner: Boolean
        get() = withValidityAssertion { descriptor.isInner }

    override val isData: Boolean
        get() = withValidityAssertion { descriptor.isData }

    override val isInline: Boolean
        get() = withValidityAssertion { descriptor.isInline || descriptor.isValue }

    override val isFun: Boolean
        get() = withValidityAssertion { descriptor.isFun }

    override val isExternal: Boolean
        get() = withValidityAssertion { descriptor.isExternal }

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val companionObject: KaNamedClassSymbol?
        get() {
            withValidityAssertion {
                val companionObject = descriptor.companionObjectDescriptor ?: return null
                return KaFe10DescNamedClassSymbol(companionObject, analysisContext)
            }
        }

    @KaExperimentalApi
    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    @OptIn(KaAnalysisApiInternals::class)
    override val classKind: KaClassKind
        get() = withValidityAssertion {
            if (DescriptorUtils.isAnonymousObject(descriptor)) {
                error("Should be an anonymous object")
            }
            descriptor.kind.toKtClassKind(isCompanionObject = descriptor.isCompanionObject)
        }

    override val superTypes: List<KaType>
        get() = withValidityAssertion {
            descriptor.getSupertypesWithAny().map { it.toKtType(analysisContext) }
        }

    override val classId: ClassId?
        get() = withValidityAssertion { descriptor.classId }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { descriptor.kaSymbolLocation }

    override val modality: KaSymbolModality
        get() = withValidityAssertion { descriptor.kaSymbolModality }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { descriptor.ktVisibility }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { descriptor.declaredTypeParameters.map { KaFe10DescTypeParameterSymbol(it, analysisContext) } }

    override fun createPointer(): KaSymbolPointer<KaNamedClassSymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaNamedClassSymbol>(this)?.let {
            return it
        }

        val classId = descriptor.classId
        if (classId != null) {
            return KaFe10DescNamedClassSymbolPointer(classId)
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}