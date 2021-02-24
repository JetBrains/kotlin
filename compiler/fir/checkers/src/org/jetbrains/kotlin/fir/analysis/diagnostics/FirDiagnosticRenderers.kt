/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.WhenMissingCase
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.render

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
        symbols.joinToString(prefix = "[", postfix = "]", separator = ", ", limit = 3, truncated = "...") { symbol ->
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

    val NAME = Renderer { element: FirElement ->
        when (element) {
            is FirMemberDeclaration -> DECLARATION_NAME.render(element)
            is FirCallableDeclaration<*> -> element.symbol.callableId.callableName.asString()
            else -> "???"
        }
    }

    val VISIBILITY = Renderer { visibility: Visibility ->
        visibility.externalDisplayName
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

    val RENDER_TYPE = Renderer { t: ConeKotlinType ->
        // TODO: need a way to tune granuality, e.g., without parameter names in functional types.
        t.render()
    }

    val FQ_NAMES_IN_TYPES = Renderer { element: FirElement ->
        element.renderWithType(mode = FirRenderer.RenderMode.WithFqNamesExceptAnnotation)
    }

    val AMBIGUOUS_CALLS = Renderer { candidates: Collection<AbstractFirBasedSymbol<*>> ->
        candidates.joinToString(separator = "\n", prefix = "\n") { symbol ->
            SYMBOL.render(symbol)
        }
    }

    private const val WHEN_MISSING_LIMIT = 7

    val WHEN_MISSING_CASES = Renderer { missingCases: List<WhenMissingCase> ->
        if (missingCases.firstOrNull() == WhenMissingCase.Unknown) {
            "'else' branch"
        } else {
            val list = missingCases.joinToString(", ", limit = WHEN_MISSING_LIMIT) { "'$it'" }
            val branches = if (missingCases.size > 1) "branches" else "branch"
            "$list $branches or 'else' branch instead"
        }
    }
}
