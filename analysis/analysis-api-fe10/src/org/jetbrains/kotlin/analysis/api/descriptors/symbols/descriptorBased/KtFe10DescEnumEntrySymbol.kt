/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescMemberSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.isEqualTo
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10DescEnumEntrySymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KtFe10NeverRestoringSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntryInitializerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFe10DescEnumEntrySymbol(
    override val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtEnumEntrySymbol(), KtEnumEntryInitializerSymbol, KtFe10DescMemberSymbol<ClassDescriptor> {
    private val enumDescriptor: ClassDescriptor
        get() = descriptor.containingDeclaration as ClassDescriptor

    override val containingEnumClassIdIfNonLocal: ClassId?
        get() = withValidityAssertion { enumDescriptor.classId }

    override val callableIdIfNonLocal: CallableId?
        get() = withValidityAssertion {
            val enumClassId = enumDescriptor.classId ?: return null
            CallableId(
                packageName = enumClassId.packageFqName,
                className = enumClassId.relativeClassName,
                callableName = descriptor.name
            )
        }

    override val returnType: KtType
        get() = withValidityAssertion { enumDescriptor.defaultType.toKtType(analysisContext) }

    override val name: Name
        get() = withValidityAssertion { descriptor.name }

    // There doesn't seem to be a way to determine if `descriptor` has a body or not, so we return an initializer even for enum entries
    // without a body.
    override val enumEntryInitializer: KtEnumEntryInitializerSymbol?
        get() = this

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtEnumEntrySymbol> = withValidityAssertion {
        KtPsiBasedSymbolPointer.createForSymbolFromSource<KtEnumEntrySymbol>(this)?.let {
            return it
        }

        val enumClassId = enumDescriptor.classId
        if (enumClassId != null) {
            return KtFe10DescEnumEntrySymbolPointer(enumClassId, descriptor.name)
        }

        return KtFe10NeverRestoringSymbolPointer()
    }

    override fun equals(other: Any?): Boolean = isEqualTo(other)
    override fun hashCode(): Int = calculateHashCode()
}