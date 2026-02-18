/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

internal class KaFirDefaultBackingFieldSymbol(
    override val analysisSession: KaFirSession,
    private val backingOwningProperty: KaFirKotlinPropertySymbol<*>,
) : KaBackingFieldSymbol(), KaFirSymbol<FirBackingFieldSymbol> {
    override val psi: PsiElement?
        get() = withValidityAssertion { null }

    override val firSymbol: FirBackingFieldSymbol
        get() {
            return backingOwningProperty.firSymbol.backingFieldSymbol
                ?: errorWithAttachment("No backing field symbol found") {
                    withFirSymbolEntry("propertySymbol", backingOwningProperty.firSymbol)
                }
        }

    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion { backingOwningProperty }

    override val isNotDefault: Boolean
        get() = withValidityAssertion { false }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.PROPERTY_BACKING_FIELD }

    override val isVal: Boolean
        get() = withValidityAssertion { backingOwningProperty.isVal }

    override val returnType: KaType
        get() = withValidityAssertion { backingOwningProperty.returnType }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { KaFirAnnotationListForDeclaration.create(firSymbol, builder) }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        KaBaseBackingFieldSymbolPointer(backingOwningProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean {
        return this === other
                || (other is KaFirDefaultBackingFieldSymbol && backingOwningProperty == other.backingOwningProperty)
    }

    override fun hashCode(): Int {
        return 31 * backingOwningProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_BACKING_FIELD
    }
}