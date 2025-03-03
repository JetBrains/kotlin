/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.entryName
import org.jetbrains.kotlin.analysis.api.fir.getAllowedPsi
import org.jetbrains.kotlin.analysis.api.fir.parameterName
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirScriptParameterSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.createOwnerPointer
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaCannotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*

internal sealed class KaFirLocalOrErrorVariableSymbol(
    final override val backingPsi: KtDeclaration?,
    final override val analysisSession: KaFirSession,
    final override val lazyFirSymbol: Lazy<FirVariableSymbol<*>>,
) : KaLocalVariableSymbol(), KaFirKtBasedSymbol<KtDeclaration, FirVariableSymbol<*>> {
    constructor(declaration: KtDeclaration, session: KaFirSession) : this(
        backingPsi = declaration,
        lazyFirSymbol = lazyFirSymbol(declaration, session),
        analysisSession = session,
    )

    constructor(symbol: FirVariableSymbol<*>, session: KaFirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? KtDeclaration,
        lazyFirSymbol = lazyOf(symbol),
        analysisSession = session,
    )

    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsi ?: firSymbol.fir.getAllowedPsi() }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    override val returnType: KaType
        get() = withValidityAssertion { firSymbol.returnType(builder) }

    override fun createPointer(): KaSymbolPointer<KaLocalVariableSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<KaLocalVariableSymbol>()?.let { return it }

        if (firSymbol.fir.source?.kind == KtFakeSourceElementKind.ScriptParameter) {
            return KaFirScriptParameterSymbolPointer(name, analysisSession.createOwnerPointer(this), this)
        }

        throw KaCannotCreateSymbolPointerForLocalLibraryDeclarationException(name.asString())
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

internal class KaFirLocalVariableSymbol : KaFirLocalOrErrorVariableSymbol {
    constructor(declaration: KtProperty, session: KaFirSession) : super(declaration, session) {
        assert(declaration.isLocal)
    }

    constructor(declaration: KtParameter, session: KaFirSession) : super(declaration, session) {
        assert(declaration.isLoopParameter || declaration.isCatchParameter)
    }

    constructor(declaration: KtDestructuringDeclarationEntry, session: KaFirSession) : super(declaration, session) {
        val parent = declaration.parent

        /**
         *  [KtDestructuringDeclaration] in incorrect positions (like class-level)
         *  still produce [KtDestructuringDeclarationEntry],
         *  but their parent is [com.intellij.psi.PsiErrorElement],
         *  so for them [KaFirErrorVariableSymbol] should be created instead.
         *
         *  Otherwise, [KtDestructuringDeclarationEntry.getParentNode] will throw an exception later.
         */
        assert(parent is KtDestructuringDeclaration)

        /**
         * Top-level destructuring declarations inside [KtScript] produce non-local
         * properties for each corresponding [KtDestructuringDeclarationEntry],
         * so [KaFirKotlinPropertySymbol.create] should be used in this case.
         *
         * This assertion checks that this is not the case here.
         *
         * In the case of the script, the first parent for [KtDestructuringDeclarationEntry]
         * will be [KtDestructuringDeclaration] (previous assertion),
         * the second one is [KtBlockExpression] and the third one is [KtScript].
         */
        assert(parent.parent?.parent !is KtScript)
    }

    constructor(symbol: FirPropertySymbol, session: KaFirSession) : super(symbol, session) {
        assert(symbol.isLocal)
    }

    override val name: Name
        get() = withValidityAssertion {
            when (val backingPsi = backingPsi) {
                null -> firSymbol.name
                is KtProperty -> backingPsi.nameAsSafeName
                is KtParameter -> backingPsi.parameterName
                is KtDestructuringDeclaration -> SpecialNames.DESTRUCT
                is KtDestructuringDeclarationEntry -> backingPsi.entryName
                else -> errorWithFirSpecificEntries("Unexpected PSI ${backingPsi::class.simpleName}", fir = firSymbol.fir)
            }
        }

    override val isVal: Boolean
        get() = withValidityAssertion {
            when (backingPsi) {
                null -> firSymbol.isVal
                is KtProperty -> !backingPsi.isVar
                is KtParameter -> !backingPsi.isMutable
                is KtDestructuringDeclaration -> !backingPsi.isVar
                is KtDestructuringDeclarationEntry -> !backingPsi.isVar
                else -> errorWithFirSpecificEntries("Unexpected PSI ${backingPsi::class.simpleName}", fir = firSymbol.fir)
            }
        }
}

internal class KaFirErrorVariableSymbol : KaFirLocalOrErrorVariableSymbol {
    constructor(symbol: FirErrorPropertySymbol, session: KaFirSession) : super(symbol, session)

    /**
     * In inapplicable places FIR creates [FirErrorPropertySymbol] with [KtDestructuringDeclaration] as a source
     */
    constructor(declaration: KtDestructuringDeclaration, session: KaFirSession) : super(declaration, session)

    override val name: Name
        get() = withValidityAssertion { FirErrorPropertySymbol.NAME }

    /**
     * Technically, the error PSI may have `var` modifier, but FIR doesn't propagate this information
     * and all [org.jetbrains.kotlin.fir.declarations.FirErrorProperty] has `val` modifier.
     */
    override val isVal: Boolean
        get() = withValidityAssertion { true }
}
