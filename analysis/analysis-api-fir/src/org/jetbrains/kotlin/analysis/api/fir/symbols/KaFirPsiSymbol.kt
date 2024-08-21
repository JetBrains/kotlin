/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForDeclaration
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseEmptyAnnotationList
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbolOfType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.psi.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * A [KaFirSymbol] that is possibly backed by some [PsiElement] and builds [firSymbol] lazily (by convention),
 * allowing some properties to be calculated without the need to build a [firSymbol].
 */
internal interface KaFirPsiSymbol<out P : PsiElement, out S : FirBasedSymbol<*>> : KaFirSymbol<S> {
    /**
     * The [PsiElement] which can be used as a source of truth for some other property implementations.
     *
     * It can be as an element from a source file, or an element from a library.
     */
    val backingPsi: P?

    /**
     * The lazy implementation of [FirBasedSymbol].
     *
     * The implementation is either built on top of [backingPsi] or provided during creation.
     *
     * @see firSymbol
     */
    val lazyFirSymbol: Lazy<S>

    /**
     * The origin should be provided without using [firSymbol], if possible.
     */
    abstract override val origin: KaSymbolOrigin

    override val firSymbol: S get() = lazyFirSymbol.value
}

internal interface KaFirKtBasedSymbol<out P : KtElement, out S : FirBasedSymbol<*>> : KaFirPsiSymbol<P, S> {
    override val origin: KaSymbolOrigin get() = withValidityAssertion { psiOrSymbolOrigin() }
}

internal fun KaFirPsiSymbol<*, *>.psiOrSymbolHashCode(): Int = backingPsi?.hashCode() ?: symbolHashCode()
internal fun KaFirPsiSymbol<*, *>.psiOrSymbolEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other::class != this::class) return false

    val backingPsi = backingPsi
    val otherBackingPsi = (other as KaFirPsiSymbol<*, *>).backingPsi
    if ((backingPsi != null || otherBackingPsi != null) && backingPsi != otherBackingPsi) return false
    if (backingPsi == null) return symbolEquals(other)

    // A Java symbol represents either source or library element at once. It cannot represent both at once.
    if (backingPsi !is KtElement) return true

    // Source PSI elements represents only source symbols
    if (!backingPsi.cameFromKotlinLibrary) return true

    // As library elements may represent both library and source symbols at once, we have to check the FIR symbol equals
    return symbolEquals(other)
}

internal fun KaFirKtBasedSymbol<*, *>.psiOrSymbolAnnotationList(): KaAnnotationList {
    val annotatedElement = backingPsi as? KtAnnotated
    if (annotatedElement?.annotationEntries?.isEmpty() == true) {
        return KaBaseEmptyAnnotationList(token)
    }

    return KaFirAnnotationListForDeclaration.create(firSymbol, builder)
}

/**
 * Currently, the compiled file can represent both library and non-library origin depending on the `preferBinary`
 * parameter from [org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache.getSession].
 *
 * So, depending on it, we may represent one decompiled file as [KaSymbolOrigin.SOURCE] and as [KaSymbolOrigin.LIBRARY]
 * at the same time.
 */
internal fun <P : KtElement> KaFirPsiSymbol<P, *>.psiOrSymbolOrigin(): KaSymbolOrigin {
    val backingPsi = backingPsi
    return when {
        backingPsi == null -> symbolOrigin()
        backingPsi.cameFromKotlinLibrary -> symbolOrigin()
        else -> KaSymbolOrigin.SOURCE
    }
}

private val KtElement.cameFromKotlinLibrary: Boolean get() = containingKtFile.isCompiled

/**
 * Executes [action] if the [backingPsi] exists and came from a source file.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> KaFirPsiSymbol<*, *>.ifSource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if ((backingPsi as? KtElement)?.cameFromKotlinLibrary != false) null else action()
}

/**
 * Potentially, we may use [backingPsi] to create [KaPsiBasedSymbolPointer] for library elements as well,
 * but it triggers AST tree calculation.
 *
 * Another potential issue: the library PSI may represent both [KaSymbolOrigin.SOURCE] and as [KaSymbolOrigin.LIBRARY],
 * so it is not so simple to distinguish between them to restore the correct symbol.
 */
internal inline fun <reified S : KaSymbol> KaFirKtBasedSymbol<*, *>.psiBasedSymbolPointerOfTypeIfSource(): KaSymbolPointer<S>? {
    return ifSource { backingPsi?.symbolPointerOfType<S>() }
}

internal inline fun <reified S : FirBasedSymbol<*>> lazyFirSymbol(
    declaration: KtDeclaration,
    session: KaFirSession,
): Lazy<S> = lazyPub {
    declaration.resolveToFirSymbolOfType<S>(session.firResolveSession)
}

/**
 * This function is a workaround for KT-70728 issue.
 *
 * The problem is that library sources share the underlying PSI with binary modules, and
 * the use site session is not enough to build the correct FIR from PSI.
 * Hence, we cannot, for instance, use [createKaValueParameters] from a library source PSI as
 * it may create a FIR from unrelated module, and we will have an inconsistency.
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> KaFirPsiSymbol<*, *>.ifNotLibrarySource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (analysisSession.useSiteModule is KaLibrarySourceModule) null else action()
}

internal fun KaFirKtBasedSymbol<KtCallableDeclaration, *>.createKaValueParameters(): List<KaValueParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.valueParameters?.map { it.symbol as KaValueParameterSymbol }
        }
    }

internal fun KaFirKtBasedSymbol<KtTypeParameterListOwner, *>.createKaTypeParameters(): List<KaTypeParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.typeParameters?.map { it.symbol }
        }
    }

internal fun KaFirKtBasedSymbol<KtDeclarationWithBody, FirCallableSymbol<*>>.createReturnType(): KaType {
    val backingPsi = backingPsi
    if (backingPsi?.hasBlockBody() == true && !backingPsi.hasDeclaredReturnType()) {
        return analysisSession.builtinTypes.unit
    }

    return firSymbol.returnType(builder)
}
