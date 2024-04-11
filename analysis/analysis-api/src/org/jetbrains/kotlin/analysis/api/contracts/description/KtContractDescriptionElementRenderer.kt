/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.contracts.description.KtContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal fun Context.renderKtContractEffectDeclaration(value: KtContractEffectDeclaration, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        when (value) {
            is KtContractCallsInPlaceContractEffectDeclaration -> {
                appendProperty(value::valueParameterReference, ::renderKtContractParameterValue)
                appendSimpleProperty(value::occurrencesRange, endWithNewLine)
            }
            is KtContractConditionalContractEffectDeclaration -> {
                appendProperty(value::effect, ::renderKtContractEffectDeclaration)
                appendProperty(value::condition, ::renderKtContractBooleanExpression, endWithNewLine)
            }
            is KtContractReturnsContractEffectDeclaration -> {
                when (value) {
                    is KtContractReturnsNotNullEffectDeclaration, is KtContractReturnsSuccessfullyEffectDeclaration -> Unit
                    is KtContractReturnsSpecificValueEffectDeclaration ->
                        appendProperty(value::value, ::renderKtContractConstantValue, endWithNewLine)
                }
            }
        }
    }

private fun Context.renderKtContractConstantValue(value: KtContractConstantValue, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        appendSimpleProperty(value::constantType, endWithNewLine)
    }

private fun Context.renderKtContractParameterValue(value: KtContractParameterValue, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        appendProperty(value::parameterSymbol, ::renderKtParameterSymbol, endWithNewLine)
    }

private fun Context.renderKtContractBooleanExpression(value: KtContractBooleanExpression, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        when (value) {
            is KtContractLogicalNotExpression -> appendProperty(value::argument, ::renderKtContractBooleanExpression, endWithNewLine)
            is KtContractBooleanConstantExpression -> appendSimpleProperty(value::booleanConstant, endWithNewLine)
            is KtContractBinaryLogicExpression -> {
                appendProperty(value::left, ::renderKtContractBooleanExpression)
                appendProperty(value::right, ::renderKtContractBooleanExpression)
                appendSimpleProperty(value::operation, endWithNewLine)
            }
            is KtContractIsInstancePredicateExpression -> {
                appendProperty(value::argument, ::renderKtContractParameterValue)
                appendProperty(value::type, renderer = { type, _ ->
                    appendLine(with(session) { symbolRenderer.renderType(analysisSession, type) })
                })
                appendSimpleProperty(value::isNegated, endWithNewLine)
            }
            is KtContractIsNullPredicateExpression -> {
                appendProperty(value::argument, ::renderKtContractParameterValue)
                appendSimpleProperty(value::isNegated, endWithNewLine)
            }
            is KtContractBooleanValueParameterExpression -> {
                appendProperty(value::parameterSymbol, ::renderKtParameterSymbol, endWithNewLine)
            }
        }
    }

private fun Context.renderKtParameterSymbol(value: KtParameterSymbol, endWithNewLine: Boolean = true) {
    val renderedValue = symbolRenderer.render(session, value)
    if (endWithNewLine) printer.appendLine(renderedValue) else printer.append(renderedValue)
}

internal data class Context(val session: KtAnalysisSession, val printer: PrettyPrinter, val symbolRenderer: DebugSymbolRenderer)

private fun PrettyPrinter.appendHeader(clazz: KClass<*>, body: PrettyPrinter.() -> Unit) {
    appendLine(clazz.simpleName + ":")
    withIndent { body() }
}

private fun <T> PrettyPrinter.appendProperty(
    prop: KProperty<T>,
    renderer: (T, Boolean) -> Unit,
    endWithNewLine: Boolean = true
) {
    appendLine(prop.name + ":")
    withIndent {
        renderer(prop.call(), endWithNewLine)
    }
}

private fun PrettyPrinter.appendSimpleProperty(prop: KProperty<Any>, endWithNewLine: Boolean = true) {
    append(prop.name + ": ")
    append(prop.call().toString())
    if (endWithNewLine) appendLine()
}
