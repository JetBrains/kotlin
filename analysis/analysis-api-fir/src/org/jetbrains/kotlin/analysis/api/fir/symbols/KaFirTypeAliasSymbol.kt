/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.location
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.visibilityByModifiers
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration

internal class KaFirTypeAliasSymbol private constructor(
    override val backingPsi: KtTypeAlias?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirTypeAliasSymbol>,
) : KaTypeAliasSymbol(), KaFirKtBasedSymbol<KtTypeAlias, FirTypeAliasSymbol> {
    constructor(declaration: KtTypeAlias, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirTypeAliasSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtTypeAlias,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: findPsi() }

    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val classId: ClassId?
        get() = withValidityAssertion { backingPsi?.getClassId() ?: firSymbol.getClassId() }

    override val visibility: KaSymbolVisibility
        get() = withValidityAssertion {
            backingPsi?.visibilityByModifiers?.asKaSymbolVisibility ?: when (val visibility = firSymbol.possiblyRawVisibility) {
                Visibilities.Unknown -> KaSymbolVisibility.PUBLIC
                else -> visibility.asKaSymbolVisibility
            }
        }

    override val compilerVisibility: Visibility
        get() = withValidityAssertion { backingPsi?.visibilityByModifiers ?: firSymbol.visibility }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion {
            createKaTypeParameters() ?: firSymbol.createRegularKtTypeParameters(builder)
        }

    override val expandedType: KaType
        get() = withValidityAssertion { builder.typeBuilder.buildKtType(firSymbol.resolvedExpandedTypeRef) }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val location: KaSymbolLocation
        get() = withValidityAssertion { backingPsi?.location ?: getSymbolKind() }

    override val isActual: Boolean
        get() = withValidityAssertion { backingPsi?.hasModifier(KtTokens.ACTUAL_KEYWORD) ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = withValidityAssertion { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaTypeAliasSymbol>()?.let { return it }

        when (val symbolKind = location) {
            KaSymbolLocation.LOCAL ->
                throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())

            KaSymbolLocation.CLASS, KaSymbolLocation.TOP_LEVEL -> KaFirClassLikeSymbolPointer(
                classId!!,
                KaTypeAliasSymbol::class,
                this
            )
            else -> throw KaUnsupportedSymbolLocation(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
