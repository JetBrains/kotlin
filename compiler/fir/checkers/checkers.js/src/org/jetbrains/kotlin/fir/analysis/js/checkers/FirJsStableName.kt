/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*

internal data class FirJsStableName(
    val name: String,
    val symbol: FirBasedSymbol<*>,
    val canBeMangled: Boolean,
    val isPresentInGeneratedCode: Boolean,
) {
    companion object {
        private fun hasPublicName(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
            return when (symbol) {
                is FirClassLikeSymbol -> !symbol.isLocal
                is FirCallableSymbol -> {
                    val parentClass = symbol.getContainingClassSymbol(session)
                    if (parentClass != null) {
                        when (symbol.visibility) {
                            is Visibilities.Public -> true
                            is Visibilities.Protected -> !parentClass.isFinal && parentClass.visibility.isPublicAPI
                            else -> false
                        }
                    } else {
                        !symbol.callableId.isLocal && symbol.effectiveVisibility.publicApi
                    }
                }
                else -> false
            }
        }

        fun createStableNameOrNull(symbol: FirBasedSymbol<*>, session: FirSession): FirJsStableName? {
            val jsName = symbol.getJsName(session)
            if (jsName != null) {
                return FirJsStableName(jsName, symbol, false, symbol.isPresentInGeneratedCode(session))
            }

            when (symbol) {
                is FirConstructorSymbol -> return null
                is FirPropertyAccessorSymbol -> return null
                // Skip type aliases since they cannot be external, cannot be exported to JavaScript, and cannot be marked with @JsName.
                // Furthermore, in the generated JavaScript code, all type alias declarations are removed,
                // and their usages are replaced with the aliased types.
                is FirTypeAliasSymbol -> return null
            }

            val hasStableNameInJavaScript = when {
                symbol.isEffectivelyExternal(session) -> true
                symbol.isExportedObject(session) -> true
                else -> false
            }

            if (hasStableNameInJavaScript ||
                // TODO: The behavior (using hasPublicName()) is inherited from K1 frontend checks for the Legacy backend.
                //  However, it is entirely unnecessary in the IR backend.
                //  This is a trying to replicate the K1 Legacy logic for the sake of consistency,
                //  but we should redesign and overhaul it later: KT-60554
                // TODO: After that, FirJsStableName.canBeMangled should always be false,
                //  and all related code can be removed, such as methods:
                //  - FirJsStableName.hasPublicName();
                //  - FirBasedSymbol<*>.doesJSManglingChangeName();
                //  - FirJsStableName.shouldClashBeCaughtByCommonFrontendCheck().
                hasPublicName(symbol, session)
            ) {
                val name = symbol.memberDeclarationNameOrNull?.identifierOrNullIfSpecial
                if (name != null) {
                    return FirJsStableName(name, symbol, !hasStableNameInJavaScript, symbol.isPresentInGeneratedCode(session))
                }
            }
            return null
        }
    }

    private fun FirBasedSymbol<*>.doesJSManglingChangeName(): Boolean {
        return when (this) {
            is FirFunctionSymbol<*> -> isExtension || valueParameterSymbols.isNotEmpty() || typeParameterSymbols.isNotEmpty()
            is FirPropertySymbol -> isExtension
            else -> false
        }
    }

    private fun shouldClashBeCaughtByCommonFrontendCheck(lhs: FirBasedSymbol<*>, rhs: FirBasedSymbol<*>): Boolean {
        return (lhs is FirFunctionSymbol<*> && rhs is FirFunctionSymbol<*>) ||
                (lhs is FirPropertySymbol && rhs is FirPropertySymbol) ||
                (lhs is FirClassLikeSymbol<*> && rhs is FirClassLikeSymbol<*>)
    }

    private fun isExternalRedeclarable(): Boolean {
        return when {
            isPresentInGeneratedCode -> false
            (symbol as? FirCallableSymbol<*>)?.isFinal == true -> true
            else -> symbol is FirClassLikeSymbol<*>
        }
    }

    fun clashesWith(other: FirJsStableName): Boolean {
        return when {
            symbol === other.symbol -> false
            name != other.name -> false
            !isPresentInGeneratedCode && !other.isPresentInGeneratedCode -> false
            isExternalRedeclarable() || other.isExternalRedeclarable() -> false
            symbol.isActual != other.symbol.isActual -> false
            symbol.isExpect != other.symbol.isExpect -> false
            canBeMangled && symbol.doesJSManglingChangeName() -> false
            other.canBeMangled && other.symbol.doesJSManglingChangeName() -> false
            canBeMangled && other.canBeMangled && shouldClashBeCaughtByCommonFrontendCheck(symbol, other.symbol) -> false
            else -> true
        }
    }
}

internal fun Collection<FirJsStableName>.collectNameClashesWith(name: FirJsStableName) = mapNotNull { next ->
    next.takeIf {
        next.clashesWith(name)
    }
}
