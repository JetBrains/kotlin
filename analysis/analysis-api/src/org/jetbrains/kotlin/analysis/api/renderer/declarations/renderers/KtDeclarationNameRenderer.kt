/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.renderer.render

@KaExperimentalApi
public interface KaDeclarationNameRenderer {
    public fun renderName(
        analysisSession: KaSession,
        symbol: KaNamedSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter
    ) {
        renderName(analysisSession, symbol.name, symbol, declarationRenderer, printer)
    }

    public fun renderName(
        analysisSession: KaSession,
        name: Name,
        symbol: KaNamedSymbol?,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object QUOTED : KaDeclarationNameRenderer {
        override fun renderName(
            analysisSession: KaSession,
            name: Name,
            symbol: KaNamedSymbol?,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            if (symbol is KaClassOrObjectSymbol && symbol.classKind == KaClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.render())
        }
    }

    public object UNQUOTED : KaDeclarationNameRenderer {
        override fun renderName(
            analysisSession: KaSession,
            name: Name,
            symbol: KaNamedSymbol?,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            if (symbol is KaClassOrObjectSymbol && symbol.classKind == KaClassKind.COMPANION_OBJECT && symbol.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
                return
            printer.append(name.asString())
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaDeclarationNameRenderer' instead", ReplaceWith("KaDeclarationNameRenderer"))
public typealias KtDeclarationNameRenderer = KaDeclarationNameRenderer