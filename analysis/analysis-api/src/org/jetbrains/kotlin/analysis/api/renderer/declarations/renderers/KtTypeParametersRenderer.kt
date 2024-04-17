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
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderTypeParameters(symbol: KtDeclarationSymbol, printer: PrettyPrinter)

    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderWhereClause(symbol: KtDeclarationSymbol, printer: PrettyPrinter)

    public object NO_TYPE_PARAMETERS : KtTypeParametersRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderTypeParameters(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
        }

        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderWhereClause(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
        }
    }

    public object WIHTOUT_BOUNDS : KtTypeParametersRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderTypeParameters(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
            val typeParameters = symbol.typeParameters
                .filter { typeParametersFilter.filter(it, symbol) }
                .ifEmpty { return }
            printer.printCollection(typeParameters, prefix = "<", postfix = ">") { typeParameter ->
                codeStyle.getSeparatorBetweenAnnotationAndOwner(typeParameter).separated(
                    { annotationRenderer.renderAnnotations(typeParameter, printer) },
                    { modifiersRenderer.renderDeclarationModifiers(typeParameter, printer) },
                    { nameRenderer.renderName(typeParameter, printer) },
                )
            }
        }

        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderWhereClause(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
        }
    }

    public object WITH_BOUNDS_IN_WHERE_CLAUSE : KtTypeParametersRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderTypeParameters(symbol: KtDeclarationSymbol, printer: PrettyPrinter) {
            val typeParameters = symbol.typeParameters
                .filter { typeParametersFilter.filter(it, symbol) }
                .ifEmpty { return }
            printer.printCollection(typeParameters, prefix = "<", postfix = ">") { typeParameter ->
                codeStyle.getSeparatorBetweenAnnotationAndOwner(typeParameter).separated(
                    { annotationRenderer.renderAnnotations(typeParameter, printer) },
                    { modifiersRenderer.renderDeclarationModifiers(typeParameter, printer) },
                    { nameRenderer.renderName(typeParameter, printer) },
                )
                if (typeParameter.upperBounds.size == 1) {
                    append(" : ")
                    val ktType = typeParameter.upperBounds.single()
                    val type = declarationTypeApproximator.approximateType(ktType, Variance.OUT_VARIANCE)
                    typeRenderer.renderType(type, printer)
                }
            }
        }

        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderWhereClause(symbol: KtDeclarationSymbol, printer: PrettyPrinter): Unit = printer {
            val allBounds = symbol.typeParameters
                .filter { typeParametersFilter.filter(it, symbol) }
                .flatMap { typeParam ->
                    if (typeParam.upperBounds.size > 1) {
                        typeParam.upperBounds.map { bound -> typeParam to bound }
                    } else {
                        emptyList()
                    }
                }.ifEmpty { return }
            " ".separated(
                {
                    keywordsRenderer.renderKeyword(KtTokens.WHERE_KEYWORD, symbol, printer)
                },
                {
                    printer.printCollection(allBounds) { (typeParameter, bound) ->
                        " : ".separated(
                            { nameRenderer.renderName(typeParameter, printer) },
                            { typeRenderer.renderType(declarationTypeApproximator.approximateType(bound, Variance.OUT_VARIANCE), printer) },
                        )
                    }
                },
            )

        }
    }
}
