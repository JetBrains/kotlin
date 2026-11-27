/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.location
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaUnsupportedSymbolLocation
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createNestedClassLikeSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.visibility
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.isActualDeclaration
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration

internal class KaFirTypeAliasSymbol private constructor(
    override val backingPsi: KtTypeAlias?,
    override val analysisSession: KaFirSession,
    override val lazyFirSymbol: Lazy<FirTypeAliasSymbol>,
) : KaTypeAliasSymbol(), KaFirKtBasedSymbol<KtTypeAlias, FirTypeAliasSymbol> {
    constructor(declaration: KtTypeAlias, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, analysisSession = session),
        analysisSession = session,
    )

    constructor(symbol: FirTypeAliasSymbol, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtTypeAlias,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val name: Name
        get() = entryPointImplementation { backingPsi?.nameAsSafeName ?: firSymbol.name }

    override val classId: ClassId?
        get() = entryPointImplementation { backingPsi?.getClassId() ?: firSymbol.getClassId() }

    override val visibility: KaSymbolVisibility
        get() = entryPointImplementation { (backingPsi?.visibility ?: firSymbol.visibility).asKaSymbolVisibility }

    override val typeParameters: List<KaTypeParameterSymbol>
        get() = entryPointImplementation {
            createKaTypeParameters() ?: firSymbol.createRegularKtTypeParameters()
        }

    override val expandedType: KaType
        get() = entryPointImplementation { firSymbolBuilder.typeBuilder.buildKtType(firSymbol.resolvedExpandedTypeRef) }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation { psiOrSymbolAnnotationList() }

    override val location: KaSymbolLocation
        get() = entryPointImplementation { backingPsi?.location ?: getSymbolKind() }

    override val isActual: Boolean
        get() = entryPointImplementation { backingPsi?.isActualDeclaration() ?: firSymbol.isActual }

    override val isExpect: Boolean
        get() = entryPointImplementation { backingPsi?.isExpectDeclaration() ?: firSymbol.isExpect }

    override fun createPointer(): KaSymbolPointer<KaTypeAliasSymbol> = entryPointImplementation {
        psiBasedSymbolPointerOfTypeIfSource<KaTypeAliasSymbol>()?.let { return it }

        when (val symbolKind = location) {
            KaSymbolLocation.LOCAL ->
                throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(classId?.asString() ?: name.asString())
            KaSymbolLocation.CLASS -> createNestedClassLikeSymbolPointer(KaTypeAliasSymbol::class)
            KaSymbolLocation.TOP_LEVEL ->
                KaFirClassLikeSymbolPointer(classId!!, KaTypeAliasSymbol::class, this)
            else -> throw KaUnsupportedSymbolLocation(this::class, symbolKind)
        }
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
