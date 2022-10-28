/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.superTypes

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtSuperTypesCallArgumentsRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSuperTypeArguments(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter)

    public object NO_ARGS : KtSuperTypesCallArgumentsRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSuperTypeArguments(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter) {
        }
    }

    public object EMPTY_PARENS : KtSuperTypesCallArgumentsRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSuperTypeArguments(type: KtType, symbol: KtClassOrObjectSymbol, printer: PrettyPrinter) {
            if ((type as? KtClassType)?.expandedClassSymbol?.classKind?.isClass != true) {
                return
            }
            printer.append("()")
        }
    }

}

