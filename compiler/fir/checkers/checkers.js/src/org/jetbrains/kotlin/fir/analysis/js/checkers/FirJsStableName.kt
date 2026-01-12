/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*

internal data class FirJsStableName(
    val name: String,
    val symbol: FirBasedSymbol<*>,
    val isPresentInGeneratedCode: Boolean,
) {
    companion object {
        context(context: CheckerContext)
        fun createStableNameOrNull(symbol: FirBasedSymbol<*>): FirJsStableName? {
            val session = context.session
            val jsName = symbol.getJsName(session)
            if (jsName != null) {
                return FirJsStableName(jsName, symbol, symbol.isPresentInGeneratedCode(session))
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
                symbol is FirCallableSymbol<*> && symbol.isEffectivelyExternalOrOverridingExternal() -> true
                symbol.isExportedObject(session) -> true
                else -> false
            }

            if (hasStableNameInJavaScript) {
                val name = symbol.memberDeclarationNameOrNull?.identifierOrNullIfSpecial
                if (name != null) {
                    return FirJsStableName(name, symbol, symbol.isPresentInGeneratedCode(session))
                }
            }

            return null
        }
    }

    private fun isExternalRedeclarable(): Boolean {
        return when {
            isPresentInGeneratedCode -> false
            (symbol as? FirCallableSymbol<*>)?.isFinal == true -> true
            else -> symbol is FirClassLikeSymbol<*>
        }
    }

    context(context: CheckerContext)
    fun clashesWith(other: FirJsStableName): Boolean {
        return when {
            symbol === other.symbol -> false
            name != other.name -> false
            !isPresentInGeneratedCode && !other.isPresentInGeneratedCode -> false
            isExternalRedeclarable() || other.isExternalRedeclarable() -> false
            symbol.isActual != other.symbol.isActual -> false
            symbol.isExpect != other.symbol.isExpect -> false
            else -> true
        }
    }
}

context(context: CheckerContext)
internal fun Collection<FirJsStableName>.collectNameClashesWith(name: FirJsStableName) = mapNotNull { next ->
    next.takeIf {
        next.clashesWith(name)
    }
}
