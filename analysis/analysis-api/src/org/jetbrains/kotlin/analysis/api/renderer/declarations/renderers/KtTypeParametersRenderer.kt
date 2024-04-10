/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.Variance

public interface KtTypeParametersRenderer {
    public fun renderTypeParameters(
        analysisSession: KtAnalysisSession,
        symbol: KtDeclarationSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public fun renderWhereClause(
        analysisSession: KtAnalysisSession,
        symbol: KtDeclarationSymbol,
        declarationRenderer: KtDeclarationRenderer,
        printer: PrettyPrinter,
    )

    public object NO_TYPE_PARAMETERS : KtTypeParametersRenderer {
        override fun renderTypeParameters(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}

        override fun renderWhereClause(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    public object WIHTOUT_BOUNDS : KtTypeParametersRenderer {
        override fun renderTypeParameters(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            val typeParameters = symbol.typeParameters
                .filter { declarationRenderer.typeParametersFilter.filter(analysisSession, it, symbol) }
                .ifEmpty { return }
            printer.printCollection(typeParameters, prefix = "<", postfix = ">") { typeParameter ->
                declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, typeParameter).separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, typeParameter, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, typeParameter, printer) },
                    { declarationRenderer.nameRenderer.renderName(analysisSession, typeParameter, declarationRenderer, printer) },
                )
            }
        }

        override fun renderWhereClause(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
        }
    }

    public object WITH_BOUNDS_IN_WHERE_CLAUSE : KtTypeParametersRenderer {
        override fun renderTypeParameters(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            val typeParameters = symbol.typeParameters
                .filter { declarationRenderer.typeParametersFilter.filter(analysisSession, it, symbol) }
                .ifEmpty { return }
            printer.printCollection(typeParameters, prefix = "<", postfix = ">") { typeParameter ->
                declarationRenderer.codeStyle.getSeparatorBetweenAnnotationAndOwner(analysisSession, typeParameter).separated(
                    { declarationRenderer.annotationRenderer.renderAnnotations(analysisSession, typeParameter, printer) },
                    { declarationRenderer.modifiersRenderer.renderDeclarationModifiers(analysisSession, typeParameter, printer) },
                    { declarationRenderer.nameRenderer.renderName(analysisSession, typeParameter, declarationRenderer, printer) },
                )
                if (typeParameter.upperBounds.size == 1) {
                    append(" : ")
                    val ktType = typeParameter.upperBounds.single()
                    val type = declarationRenderer.declarationTypeApproximator.approximateType(analysisSession, ktType, Variance.OUT_VARIANCE)
                    declarationRenderer.typeRenderer.renderType(analysisSession, type, printer)
                }
            }
        }

        override fun renderWhereClause(
            analysisSession: KtAnalysisSession,
            symbol: KtDeclarationSymbol,
            declarationRenderer: KtDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val allBounds = symbol.typeParameters
                    .filter { declarationRenderer.typeParametersFilter.filter(analysisSession, it, symbol) }
                    .flatMap { typeParam ->
                        if (typeParam.upperBounds.size > 1) {
                            typeParam.upperBounds.map { bound -> typeParam to bound }
                        } else {
                            emptyList()
                        }
                    }.ifEmpty { return }
                " ".separated(
                    {
                        declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.WHERE_KEYWORD, symbol, printer)
                    },
                    {
                        printer.printCollection(allBounds) { (typeParameter, bound) ->
                            " : ".separated(
                                { declarationRenderer.nameRenderer.renderName(analysisSession, typeParameter, declarationRenderer, printer) },
                                {
                                    val approximatedType = declarationRenderer.declarationTypeApproximator
                                        .approximateType(analysisSession, bound, Variance.OUT_VARIANCE)

                                    declarationRenderer.typeRenderer.renderType(analysisSession, approximatedType, printer) }
                                ,
                            )
                        }
                    },
                )

            }
        }
    }
}
