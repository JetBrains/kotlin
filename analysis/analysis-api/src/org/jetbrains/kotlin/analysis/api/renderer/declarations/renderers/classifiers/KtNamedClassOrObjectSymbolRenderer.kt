/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaNamedClassOrObjectSymbolRenderer {
    public fun renderSymbol(
        analysisSession: KaSession,
        symbol: KaNamedClassOrObjectSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE: AsSourceRenderer(true)
    public object AS_SOURCE_WITHOUT_PRIMARY_CONSTRUCTOR: AsSourceRenderer(false)

    public open class AsSourceRenderer(private val withPrimaryConstructor: Boolean) : KaNamedClassOrObjectSymbolRenderer {
        override fun renderSymbol(
            analysisSession: KaSession,
            symbol: KaNamedClassOrObjectSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val keywords = when (symbol.classKind) {
                    KaClassKind.CLASS -> listOf(KtTokens.CLASS_KEYWORD)
                    KaClassKind.ENUM_CLASS -> listOf(KtTokens.ENUM_KEYWORD, KtTokens.CLASS_KEYWORD)
                    KaClassKind.ANNOTATION_CLASS -> listOf(KtTokens.ANNOTATION_KEYWORD, KtTokens.CLASS_KEYWORD)
                    KaClassKind.OBJECT -> listOf(KtTokens.OBJECT_KEYWORD)
                    KaClassKind.COMPANION_OBJECT -> listOf(KtTokens.COMPANION_KEYWORD, KtTokens.OBJECT_KEYWORD)
                    KaClassKind.INTERFACE -> listOf(KtTokens.INTERFACE_KEYWORD)
                    KaClassKind.ANONYMOUS_OBJECT -> error("KaNamedClassOrObjectSymbol cannot be KaAnonymousObjectSymbol")
                }

                " ".separated(
                    { renderAnnotationsModifiersAndContextReceivers(analysisSession, symbol, declarationRenderer, printer, keywords) },
                    {
                        val primaryConstructor = if (withPrimaryConstructor)
                            declarationRenderer.bodyMemberScopeProvider.getMemberScope(analysisSession, symbol).filterIsInstance<KaConstructorSymbol>()
                                .firstOrNull { it.isPrimary }
                        else null

                        declarationRenderer.nameRenderer.renderName(analysisSession, symbol, declarationRenderer, printer)
                        declarationRenderer.typeParametersRenderer.renderTypeParameters(analysisSession, symbol, declarationRenderer, printer)
                        if (primaryConstructor != null) {
                            val annotationsPrinted = checkIfPrinted {
                                renderAnnotationsModifiersAndContextReceivers(analysisSession, primaryConstructor, declarationRenderer, printer)
                            }
                            if (annotationsPrinted) {
                                withPrefix(" ") {
                                    declarationRenderer.keywordsRenderer
                                        .renderKeyword(analysisSession, KtTokens.CONSTRUCTOR_KEYWORD, primaryConstructor, printer)
                                }
                            }
                            if (primaryConstructor.valueParameters.isNotEmpty()) {
                                declarationRenderer.valueParametersRenderer.renderValueParameters(analysisSession, primaryConstructor, declarationRenderer, printer)
                            }
                        }
                    },
                    {
                        declarationRenderer.typeParametersRenderer
                            .renderWhereClause(analysisSession, symbol, declarationRenderer, printer)
                    },
                    {
                        withPrefix(": ") {
                            declarationRenderer.superTypeListRenderer
                                .renderSuperTypes(analysisSession, symbol, declarationRenderer, printer)
                        }
                    },
                    {
                        declarationRenderer.classifierBodyRenderer
                            .renderBody(analysisSession, symbol, declarationRenderer, printer)
                    }
                )
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaNamedClassOrObjectSymbolRenderer' instead", ReplaceWith("KaNamedClassOrObjectSymbolRenderer"))
public typealias KtNamedClassOrObjectSymbolRenderer = KaNamedClassOrObjectSymbolRenderer