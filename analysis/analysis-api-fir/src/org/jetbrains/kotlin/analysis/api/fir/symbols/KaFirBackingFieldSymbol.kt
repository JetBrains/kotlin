/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseBackingFieldSymbolPointer
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.psi.KtBackingField

internal class KaFirBackingFieldSymbol private constructor(
    override val backingPsi: KtBackingField?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirBackingFieldSymbol>,
    private val backingOwningProperty: KaFirKotlinPropertySymbol<*>,
) : KaBackingFieldSymbol(), KaFirKtBasedSymbol<KtBackingField, FirBackingFieldSymbol> {
    constructor(declaration: KtBackingField, session: KaFirSession, owningProperty: KaFirKotlinPropertySymbol<*>) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
        backingOwningProperty = owningProperty,
    )

    constructor(symbol: FirBackingFieldSymbol, session: KaFirSession, owningProperty: KaFirKotlinPropertySymbol<*>) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtBackingField,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
        backingOwningProperty = owningProperty
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi }

    override val owningProperty: KaKotlinPropertySymbol
        get() = withValidityAssertion { backingOwningProperty }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { super<KaBackingFieldSymbol>.origin }

    override val isVal: Boolean
        get() = withValidityAssertion { backingOwningProperty.isVal }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(analysisSession.firSymbolBuilder) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            if (!backingPsi?.annotationEntries.isNullOrEmpty() || backingOwningProperty.mayHaveBackingFieldAnnotation()) {
                KaFirAnnotationListForDeclaration.create(firSymbol, builder)
            } else {
                KaBaseEmptyAnnotationList(token)
            }
        }

    override fun createPointer(): KaSymbolPointer<KaBackingFieldSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaBackingFieldSymbol>()
            ?: KaBaseBackingFieldSymbolPointer(backingOwningProperty.createPointer(), this)
    }

    override fun equals(other: Any?): Boolean {
        return this === other
                || (other is KaFirBackingFieldSymbol && backingOwningProperty == other.backingOwningProperty)
    }

    override fun hashCode(): Int {
        return 31 * backingOwningProperty.hashCode() + KaFirKotlinPropertySymbol.HASH_CODE_ADDITION_FOR_BACKING_FIELD
    }
}

/**
 * Checks whether the property may have annotations on its backing field without performing any resolution.
 *
 * The function is tailored for source properties with a backing PSI.
 * The compiler preserves annotations on backing fields for properties coming from libraries, so for them the FIR tree needs to be
 * checked directly. However, the FIR tree for compiled declarations is already resolved, so a direct check is virtually free.
 */
private fun KaFirKotlinPropertySymbol<*>.mayHaveBackingFieldAnnotation(): Boolean {
    val annotationEntries = backingPsi?.annotationEntries ?: return false
    return annotationEntries.any {
        when (it.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null, AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD, AnnotationUseSiteTarget.ALL -> true
            else -> false
        }
    }
}