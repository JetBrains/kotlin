/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.*
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.*
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

@KaNonPublicApi
internal fun Context.renderKaContractEffectDeclaration(value: KaContractEffectDeclaration, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        when (value) {
            is KaContractCallsInPlaceContractEffectDeclaration -> {
                appendProperty(value::valueParameterReference, ::renderKaContractParameterValue)
                appendSimpleProperty(value::occurrencesRange, endWithNewLine)
            }
            is KaContractConditionalContractEffectDeclaration -> {
                appendProperty(value::effect, ::renderKaContractEffectDeclaration)
                appendProperty(value::condition, ::renderKaContractBooleanExpression, endWithNewLine)
            }
            is KaContractReturnsContractEffectDeclaration -> {
                when (value) {
                    is KaContractReturnsNotNullEffectDeclaration, is KaContractReturnsSuccessfullyEffectDeclaration -> Unit
                    is KaContractReturnsSpecificValueEffectDeclaration ->
                        appendProperty(value::value, ::renderKaContractConstantValue, endWithNewLine)
                }
            }
        }
    }

@KaNonPublicApi
private fun Context.renderKaContractConstantValue(value: KaContractConstantValue, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        appendSimpleProperty(value::constantType, endWithNewLine)
    }

@KaNonPublicApi
private fun Context.renderKaContractParameterValue(value: KaContractParameterValue, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        appendProperty(value::parameterSymbol, ::renderKaParameterSymbol, endWithNewLine)
    }

@KaNonPublicApi
private fun Context.renderKaContractBooleanExpression(value: KaContractBooleanExpression, endWithNewLine: Boolean = true): Unit =
    printer.appendHeader(value::class) {
        when (value) {
            is KaContractLogicalNotExpression -> appendProperty(value::argument, ::renderKaContractBooleanExpression, endWithNewLine)
            is KaContractBooleanConstantExpression -> appendSimpleProperty(value::booleanConstant, endWithNewLine)
            is KaContractBinaryLogicExpression -> {
                appendProperty(value::left, ::renderKaContractBooleanExpression)
                appendProperty(value::right, ::renderKaContractBooleanExpression)
                appendSimpleProperty(value::operation, endWithNewLine)
            }
            is KaContractIsInstancePredicateExpression -> {
                appendProperty(value::argument, ::renderKaContractParameterValue)
                appendProperty(value::type, renderer = { type, _ ->
                    appendLine(with(session) { symbolRenderer.renderType(useSiteSession, type) })
                })
                appendSimpleProperty(value::isNegated, endWithNewLine)
            }
            is KaContractIsNullPredicateExpression -> {
                appendProperty(value::argument, ::renderKaContractParameterValue)
                appendSimpleProperty(value::isNegated, endWithNewLine)
            }
            is KaContractBooleanValueParameterExpression -> {
                appendProperty(value::parameterSymbol, ::renderKaParameterSymbol, endWithNewLine)
            }
        }
    }

@KaNonPublicApi
private fun Context.renderKaParameterSymbol(value: KaParameterSymbol, endWithNewLine: Boolean = true) {
    val renderedValue = symbolRenderer.render(session, value)
    if (endWithNewLine) printer.appendLine(renderedValue) else printer.append(renderedValue)
}

@KaNonPublicApi
internal data class Context(val session: KaSession, val printer: PrettyPrinter, val symbolRenderer: DebugSymbolRenderer)

private fun PrettyPrinter.appendHeader(clazz: KClass<*>, body: PrettyPrinter.() -> Unit) {
    append(clazz.simpleName)
    appendLine(":")
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
