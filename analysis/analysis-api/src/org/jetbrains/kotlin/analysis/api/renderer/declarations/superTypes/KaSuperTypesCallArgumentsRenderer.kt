/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaSuperTypesCallArgumentsRenderer {
    public fun renderSuperTypeArguments(
        analysisSession: KaSession,
        type: KaType,
        symbol: KaClassSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object NO_ARGS : KaSuperTypesCallArgumentsRenderer {
        override fun renderSuperTypeArguments(
            analysisSession: KaSession,
            type: KaType,
            symbol: KaClassSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
        }
    }

    @KaExperimentalApi
    public object EMPTY_PARENS : KaSuperTypesCallArgumentsRenderer {
        override fun renderSuperTypeArguments(
            analysisSession: KaSession,
            type: KaType,
            symbol: KaClassSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            with(analysisSession) {
                if (type.expandedSymbol?.classKind?.isClass != true) {
                    return
                }
                printer.append("()")
            }
        }
    }
}
