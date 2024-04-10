/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.renderer.render

public interface KtDeclarationNameRenderer {
    public fun renderName(
        analysisSession: KtAnalysisSession,
        symbol: KtNamedSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter
    ) {
        renderName(analysisSession, symbol.name, symbol, declarationRenderer, printer)
    }

    public fun renderName(
        analysisSession: KtAnalysisSession,
        name: Name,
        symbol: KtNamedSymbol?,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object QUOTED : KtDeclarationNameRenderer {
        override fun renderName(
            analysisSession: KtAnalysisSession,
            name: Name,
            symbol: KtNamedSymbol?,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            if (symbol is KtClassOrObjectSymbol && symbol.classKind == KtClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.render())
        }
    }

    public object UNQUOTED : KtDeclarationNameRenderer {
        override fun renderName(
            analysisSession: KtAnalysisSession,
            name: Name,
            symbol: KtNamedSymbol?,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            if (symbol is KtClassOrObjectSymbol && symbol.classKind == KtClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.asString())
        }
    }
}
