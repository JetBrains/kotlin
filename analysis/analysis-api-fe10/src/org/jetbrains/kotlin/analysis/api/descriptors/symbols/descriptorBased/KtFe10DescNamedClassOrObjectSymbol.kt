/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescNamedClassOrObjectSymbolSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.toKtClassKind
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolKind
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
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

    override val isActual: Boolean
        get() = withValidityAssertion { descriptor.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { descriptor.isExpect }

    override val companionObject: KtNamedClassOrObjectSymbol?
        get() {
            withValidityAssertion {
                val companionObject = descriptor.companionObjectDescriptor ?: return null
                return KtFe10DescNamedClassOrObjectSymbol(companionObject, analysisContext)
            }
        }

    override val contextReceivers: List<KtContextReceiver>
        get() = withValidityAssertion { descriptor.createContextReceivers(analysisContext) }

    @OptIn(KtAnalysisApiInternals::class)
    override val classKind: KtClassKind
        get() = withValidityAssertion {
            if (DescriptorUtils.isAnonymousObject(descriptor)) {
                error("Should be an anonymous object")
            }
            descriptor.kind.toKtClassKind(isCompanionObject = descriptor.isCompanionObject)
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

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtNamedClassOrObjectSymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtNamedClassOrObjectSymbol>(this)?.let {
            return it
        }

        val classId = descriptor.classId
        if (classId != null) {
            return KtFe10DescNamedClassOrObjectSymbolSymbol(classId)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}