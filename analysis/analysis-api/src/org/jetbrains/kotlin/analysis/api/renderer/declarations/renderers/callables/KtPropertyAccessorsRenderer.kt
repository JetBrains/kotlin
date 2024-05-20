/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.callables

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public interface KaPropertyAccessorsRenderer {
    public fun renderAccessors(
        analysisSession: KaSession,
        symbol: KaPropertySymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object ALL : KaPropertyAccessorsRenderer {
        override fun renderAccessors(
            analysisSession: KaSession,
            symbol: KaPropertySymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val toRender = listOfNotNull(symbol.getter, symbol.setter).ifEmpty { return }
                append("\n")
                withIndent {
                    "\n".separated(
                        {
                            toRender.firstIsInstanceOrNull<KaPropertyGetterSymbol>()
                                ?.let { declarationRenderer.getterRenderer.renderSymbol(analysisSession, it, declarationRenderer, printer) }
                        },
                        {
                            toRender.firstIsInstanceOrNull<KaPropertySetterSymbol>()
                                ?.let { declarationRenderer.setterRenderer.renderSymbol(analysisSession, it, declarationRenderer, printer) }
                        },
                    )
                }
            }
        }
    }

    public object NO_DEFAULT : KaPropertyAccessorsRenderer {
        override fun renderAccessors(
            analysisSession: KaSession,
            symbol: KaPropertySymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            val toRender = listOfNotNull(symbol.getter, symbol.setter)
                .filter { !it.isDefault || it.annotations.isNotEmpty() }
                .ifEmpty { return }
            append("\n")
            withIndent {
                "\n".separated(
                    {
                        toRender.firstIsInstanceOrNull<KaPropertyGetterSymbol>()
                            ?.let { declarationRenderer.getterRenderer.renderSymbol(analysisSession, it, declarationRenderer, printer) }
                    },
                    {
                        toRender.firstIsInstanceOrNull<KaPropertySetterSymbol>()
                            ?.let { declarationRenderer.setterRenderer.renderSymbol(analysisSession, it, declarationRenderer, printer) }
                    },
                )
            }

        }
    }


    public object NONE : KaPropertyAccessorsRenderer {
        override fun renderAccessors(
            analysisSession: KaSession,
            symbol: KaPropertySymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

}

public typealias KtPropertyAccessorsRenderer = KaPropertyAccessorsRenderer