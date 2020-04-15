/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

object FirDiagnosticRenderers {
    val NULLABLE_STRING = Renderer<String?> { it ?: "null" }

    val SYMBOLS = Renderer { symbols: Collection<AbstractFirBasedSymbol<*>> ->
        symbols.joinToString(prefix = "[", postfix = "]", separator = ",", limit = 3, truncated = "...") { symbol ->
            when (symbol) {
                is FirClassLikeSymbol<*> -> symbol.classId.asString()
                is FirCallableSymbol<*> -> symbol.callableId.toString()
                else -> "???"
            }
        }
    }

    val TO_STRING = Renderer { element: Any ->
        element.toString()
    }
}