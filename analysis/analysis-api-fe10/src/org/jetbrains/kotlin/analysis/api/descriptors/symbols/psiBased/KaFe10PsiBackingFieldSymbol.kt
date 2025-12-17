/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.calculateHashCode
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.createErrorType
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFe10PsiBackingFieldSymbol(
    override val psi: KtBackingField,
    override val analysisContext: Fe10AnalysisContext,
    private val backingOwningProperty: KaKotlinPropertySymbol,
) : KaBackingFieldSymbol(), KaFe10PsiSymbol<KtBackingField, PropertyDescriptor> {
    override val descriptor: PropertyDescriptor? by cached {
        when (backingOwningProperty) {
            is KaFe10PsiKotlinPropertySymbol -> backingOwningProperty.descriptor
            is KaFe10DescKotlinPropertySymbol -> backingOwningProperty.descriptor
            else -> errorWithAttachment("Unexpected property symbol type ${backingOwningProperty::class.qualifiedName}") {
                withPsiEntry("psi", psi)
            }
        }
    }

    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion { backingOwningProperty }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { super<KaBackingFieldSymbol>.origin }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> {
        val ptr = backingOwningProperty.createPointer()
        return KaBaseBackingFieldSymbolPointer(ptr, this)
    }

    override val isVal: Boolean
        get() = withValidityAssertion { backingOwningProperty.isVal }

    override val returnType: KaType
        get() = withValidityAssertion {
            // Explicit backing fields aren't supported in the old frontend.
            // So here we return the return type of the containing property.
            descriptor?.returnType?.toKtType(analysisContext) ?: createErrorType()
        }

    override fun equals(other: Any?): Boolean {
        return this === other
                || (other is KaFe10PsiBackingFieldSymbol && backingOwningProperty == other.backingOwningProperty)
    }

    override fun hashCode(): Int = calculateHashCode()
}