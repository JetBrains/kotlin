/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public interface KtPropertyAccessorsRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderAccessors(symbol: KtPropertySymbol, printer: PrettyPrinter)

    public object ALL : KtPropertyAccessorsRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderAccessors(symbol: KtPropertySymbol, printer: PrettyPrinter): Unit = printer {
            val toRender = listOfNotNull(symbol.getter, symbol.setter).ifEmpty { return }
            append("\n")
            withIndent {
                "\n".separated(
                    { toRender.firstIsInstanceOrNull<KtPropertyGetterSymbol>()?.let { getterRenderer.renderSymbol(it, printer) } },
                    { toRender.firstIsInstanceOrNull<KtPropertySetterSymbol>()?.let { setterRenderer.renderSymbol(it, printer) } },
                )
            }
        }
    }

    public object NO_DEFAULT : KtPropertyAccessorsRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderAccessors(symbol: KtPropertySymbol, printer: PrettyPrinter): Unit = printer {
            val toRender = listOfNotNull(symbol.getter, symbol.setter)
                .filter { !it.isDefault || it.annotations.isNotEmpty() }
                .ifEmpty { return }
            append("\n")
            withIndent {
                "\n".separated(
                    { toRender.firstIsInstanceOrNull<KtPropertyGetterSymbol>()?.let { getterRenderer.renderSymbol(it, printer) } },
                    { toRender.firstIsInstanceOrNull<KtPropertySetterSymbol>()?.let { setterRenderer.renderSymbol(it, printer) } },
                )
            }

        }
    }


    public object NONE : KtPropertyAccessorsRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderAccessors(symbol: KtPropertySymbol, printer: PrettyPrinter) {
        }
    }

}