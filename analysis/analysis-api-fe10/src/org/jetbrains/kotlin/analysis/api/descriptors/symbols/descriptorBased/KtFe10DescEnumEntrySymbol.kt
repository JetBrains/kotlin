/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KaFe10DescMemberSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10DescEnumEntrySymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class KaFe10DescEnumEntrySymbol(
    override val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaEnumEntrySymbol(), KaEnumEntryInitializerSymbol, KaFe10DescMemberSymbol<ClassDescriptor> {
    private val enumDescriptor: ClassDescriptor
        get() = descriptor.containingDeclaration as ClassDescriptor

    override val callableId: CallableId?
        get() = withValidityAssertion {
            val enumClassId = enumDescriptor.classId ?: return null
            CallableId(
                packageName = enumClassId.packageFqName,
                className = enumClassId.relativeClassName,
                callableName = descriptor.name
            )
        }

    override val returnType: KaType
        get() = withValidityAssertion { enumDescriptor.defaultType.toKtType(analysisContext) }

    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    // There doesn't seem to be a way to determine if `descriptor` has a body or not, so we return an initializer even for enum entries
    // without a body.
    override val enumEntryInitializer: KaEnumEntryInitializerSymbol?
        get() = this

    override fun createPointer(): KaSymbolPointer<KaEnumEntrySymbol> = withValidityAssertion {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaEnumEntrySymbol>(this)?.let {
            return it
        }

        val enumClassId = enumDescriptor.classId
        if (enumClassId != null) {
            return KaFe10DescEnumEntrySymbolPointer(enumClassId, descriptor.name)
        }

        return KaFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}