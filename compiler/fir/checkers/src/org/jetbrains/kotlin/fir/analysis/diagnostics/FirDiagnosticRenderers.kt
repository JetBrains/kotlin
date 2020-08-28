/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object FirDiagnosticRenderers {
    val NULLABLE_STRING = Renderer<String?> { it ?: "null" }

    val SYMBOL = Renderer { symbol: AbstractFirBasedSymbol<*> ->
        when (symbol) {
            is FirClassLikeSymbol<*> -> symbol.classId.asString()
            is FirCallableSymbol<*> -> symbol.callableId.toString()
            else -> "???"
        }
    }

    val SYMBOLS = Renderer { symbols: Collection<AbstractFirBasedSymbol<*>> ->
        symbols.joinToString(prefix = "[", postfix = "]", separator = ",", limit = 3, truncated = "...") { symbol ->
            SYMBOL.render(symbol)
        }
    }

    val TO_STRING = Renderer { element: Any ->
        element.toString()
    }

    val PROPERTY_NAME = Renderer { symbol: FirPropertySymbol ->
        symbol.fir.name.asString()
    }

    val FIR = Renderer { element: FirElement ->
        element.render()
    }

    val DECLARATION_NAME = Renderer { declaration: FirMemberDeclaration ->
        val name = when (declaration) {
            is FirProperty -> declaration.name
            is FirSimpleFunction -> declaration.name
            is FirRegularClass -> declaration.name
            is FirTypeAlias -> declaration.name
            is FirEnumEntry -> declaration.name
            is FirField -> declaration.name
            is FirConstructor -> return@Renderer "constructor"
            else -> return@Renderer "???"
        }
        name.asString()
    }
}
