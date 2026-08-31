/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.withSymbolAttachment
import org.jetbrains.kotlin.analysis.api.impl.base.util.requireIsInstance
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolLocation
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.FirCallableSignature
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.reflect.KClass

internal inline fun <reified D : FirCallableDeclaration> FirScope.findDeclarationWithSignature(
    signature: FirCallableSignature,
    processor: FirScope.((FirBasedSymbol<*>) -> Unit) -> Unit,
): D? {
    var foundSymbol: D? = null
    processor { symbol ->
        val declaration = symbol.fir
        if (declaration is D && signature.hasTheSameSignature(declaration)) {
            foundSymbol = declaration
        }
    }

    return foundSymbol
}

internal inline fun <reified D : FirCallableDeclaration> Collection<FirCallableSymbol<*>>.findDeclarationWithSignatureBySymbols(
    signature: FirCallableSignature,
): D? {
    for (symbol in this) {
        val declaration = symbol.fir
        if (declaration is D && signature.hasTheSameSignature(declaration)) {
            return declaration
        }
    }

    return null
}

internal fun KaFirSession.getClassLikeSymbol(classId: ClassId) =
    firSession.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir

context(sessionOwner: KaFirSymbol<*>)
internal inline fun <reified T : KaSymbol> KaSymbol.createOwnerPointer(): KaSymbolPointer<T> {
    val containingSymbol = context(sessionOwner.analysisSession) { this.containingDeclaration }
        ?: errorWithAttachment("Non-null containingDeclaration is expected for a member declaration for `${this@createOwnerPointer::class}`, expecting `${T::class}` type of owner") {
            withSymbolAttachment("child", sessionOwner.analysisSession, this@createOwnerPointer)
        }

    requireIsInstance<T>(containingSymbol)

    @Suppress("UNCHECKED_CAST")
    return containingSymbol.createPointer() as KaSymbolPointer<T>
}

/**
 * Creates a pointer for a class-like symbol with the [KaSymbolLocation.CLASS] location.
 *
 * @see KaFirClassLikeSymbolPointer
 * @see KaFirNestedInLocalClassSymbolPointer
 */
context(sessionOwner: KaFirSymbol<*>)
internal fun <T> T.createNestedClassLikeSymbolPointer(expectedClass: KClass<T>): KaSymbolPointer<T>
        where T : KaClassLikeSymbol, T : KaNamedSymbol {
    val classId = classId ?: return KaFirNestedInLocalClassSymbolPointer(
        containingClassPointer = this.createOwnerPointer<KaNamedClassSymbol>(),
        name = name,
        firOrigin = firSymbol.origin,
        expectedClass = expectedClass,
        originalSymbol = this,
    )

    return KaFirClassLikeSymbolPointer(classId, expectedClass, this)
}
