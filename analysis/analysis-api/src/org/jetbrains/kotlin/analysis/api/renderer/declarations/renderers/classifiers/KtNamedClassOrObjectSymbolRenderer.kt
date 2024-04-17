/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers.classifiers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.renderAnnotationsModifiersAndContextReceivers
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

public interface KtNamedClassOrObjectSymbolRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderSymbol(symbol: KtNamedClassOrObjectSymbol, printer: PrettyPrinter)

    public object AS_SOURCE: AsSourceRenderer(true)
    public object AS_SOURCE_WITHOUT_PRIMARY_CONSTRUCTOR: AsSourceRenderer(false)

    public open class AsSourceRenderer(private val withPrimaryConstructor: Boolean) : KtNamedClassOrObjectSymbolRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderSymbol(symbol: KtNamedClassOrObjectSymbol, printer: PrettyPrinter): Unit = printer {
            val keywords = when (symbol.classKind) {
                KtClassKind.CLASS -> listOf(KtTokens.CLASS_KEYWORD)
                KtClassKind.ENUM_CLASS -> listOf(KtTokens.ENUM_KEYWORD, KtTokens.CLASS_KEYWORD)
                KtClassKind.ANNOTATION_CLASS -> listOf(KtTokens.ANNOTATION_KEYWORD, KtTokens.CLASS_KEYWORD)
                KtClassKind.OBJECT -> listOf(KtTokens.OBJECT_KEYWORD)
                KtClassKind.COMPANION_OBJECT -> listOf(KtTokens.COMPANION_KEYWORD, KtTokens.OBJECT_KEYWORD)
                KtClassKind.INTERFACE -> listOf(KtTokens.INTERFACE_KEYWORD)
                KtClassKind.ANONYMOUS_OBJECT -> error("KtNamedClassOrObjectSymbol cannot be KtAnonymousObjectSymbol")
            }

            " ".separated(
                { renderAnnotationsModifiersAndContextReceivers(symbol, printer, keywords) },
                {
                    val primaryConstructor = if (withPrimaryConstructor)
                        bodyMemberScopeProvider.getMemberScope(symbol).filterIsInstance<KtConstructorSymbol>()
                            .firstOrNull { it.isPrimary }
                    else null

                    nameRenderer.renderName(symbol, printer)
                    typeParametersRenderer.renderTypeParameters(symbol, printer)
                    if (primaryConstructor != null) {
                        val annotationsPrinted = checkIfPrinted { renderAnnotationsModifiersAndContextReceivers(primaryConstructor, printer) }
                        if (annotationsPrinted) {
                            withPrefix(" ") {
                                keywordsRenderer.renderKeyword(KtTokens.CONSTRUCTOR_KEYWORD, primaryConstructor, printer)
                            }
                        }
                        if (primaryConstructor.valueParameters.isNotEmpty()) {
                            valueParametersRenderer.renderValueParameters(primaryConstructor, printer)
                        }
                    }
                },
                { typeParametersRenderer.renderWhereClause(symbol, printer) },
                { withPrefix(": ") { superTypeListRenderer.renderSuperTypes(symbol, printer) } },
                { classifierBodyRenderer.renderBody(symbol, printer) }
            )
        }
    }
}
