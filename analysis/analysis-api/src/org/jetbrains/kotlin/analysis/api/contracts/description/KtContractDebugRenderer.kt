/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.contracts.description.KtDebugContractRenderer.Context
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

internal class KtDebugContractRenderer(
    private val session: KtAnalysisSession,
    private val symbolRenderer: DebugSymbolRenderer
) : KtContractDescriptionVisitor<Unit, Context> {
    override fun visitConditionalEffectDeclaration(conditionalEffect: KtConditionalEffectDeclaration, data: Context) =
        data.appendHeader(conditionalEffect::class) {
            appendProperty(conditionalEffect::effect, endWithNewLine = true)
            appendProperty(conditionalEffect::condition, endWithNewLine = data.endWithNewLine)
        }

    override fun visitReturnsEffectDeclaration(returnsEffect: KtReturnsEffectDeclaration, data: Context) =
        data.appendHeader(returnsEffect::class) {
            appendProperty(returnsEffect::value, endWithNewLine = data.endWithNewLine)
        }

    override fun visitCallsEffectDeclaration(callsEffect: KtCallsEffectDeclaration, data: Context) =
        data.appendHeader(callsEffect::class) {
            appendProperty(callsEffect::valueParameterReference, endWithNewLine = true)
            appendSimpleProperty(callsEffect::kind, endWithNewLine = data.endWithNewLine)
        }

    override fun visitLogicalBinaryOperationContractExpression(binaryLogicExpression: KtBinaryLogicExpression, data: Context) {
        data.appendHeader(KtBinaryLogicExpression::class) {
            appendProperty(binaryLogicExpression::left, endWithNewLine = true)
            appendProperty(binaryLogicExpression::right, endWithNewLine = true)
            appendSimpleProperty(binaryLogicExpression::kind, endWithNewLine = data.endWithNewLine)
        }
    }

    override fun visitLogicalNot(logicalNot: KtLogicalNot, data: Context) =
        data.appendHeader(logicalNot::class) {
            appendProperty(logicalNot::arg, endWithNewLine = true)
        }

    override fun visitIsInstancePredicate(isInstancePredicate: KtIsInstancePredicate, data: Context) =
        data.appendHeader(isInstancePredicate::class) {
            appendProperty(isInstancePredicate::arg, endWithNewLine = true)
            with(session) { symbolRenderer.renderType(isInstancePredicate.type) }
            appendSimpleProperty(isInstancePredicate::isNegated, endWithNewLine = data.endWithNewLine)
        }

    override fun visitIsNullPredicate(isNullPredicate: KtIsNullPredicate, data: Context) =
        data.appendHeader(isNullPredicate::class) {
            appendProperty(isNullPredicate::arg, endWithNewLine = true)
            appendSimpleProperty(isNullPredicate::isNegated, endWithNewLine = data.endWithNewLine)
        }

    override fun visitConstantDescriptor(constantReference: KtConstantReference, data: Context) =
        data.appendHeader(constantReference::class) {
            appendSimpleProperty(constantReference::name, endWithNewLine = data.endWithNewLine)
        }

    override fun visitBooleanConstantDescriptor(booleanConstantDescriptor: KtBooleanConstantReference, data: Context) =
        data.appendHeader(booleanConstantDescriptor::class) {
            appendSimpleProperty(booleanConstantDescriptor::name, endWithNewLine = data.endWithNewLine)
        }

    override fun visitValueParameterReference(valueParameterReference: KtValueParameterReference, data: Context) =
        data.appendHeader(valueParameterReference::class) {
            appendSimpleProperty(valueParameterReference::name, endWithNewLine = data.endWithNewLine)
        }

    override fun visitBooleanValueParameterReference(
        booleanValueParameterReference: KtBooleanValueParameterReference,
        data: Context
    ) =
        data.appendHeader(booleanValueParameterReference::class) {
            appendSimpleProperty(booleanValueParameterReference::name, endWithNewLine = data.endWithNewLine)
        }

    private fun Context.appendHeader(clazz: KClass<*>, body: PrettyPrinter.() -> Unit) {
        printer.appendLine(clazz.simpleName + ":")
        printer.withIndent { body() }
    }

    private fun PrettyPrinter.appendProperty(prop: KProperty<KtContractDescriptionElement>, endWithNewLine: Boolean) {
        appendLine(prop.name + ":")
        withIndent {
            prop.call().accept(this@KtDebugContractRenderer, Context(this@appendProperty, endWithNewLine))
        }
    }

    data class Context(val printer: PrettyPrinter, val endWithNewLine: Boolean)

    private fun PrettyPrinter.appendSimpleProperty(prop: KProperty<Any>, endWithNewLine: Boolean) {
        append(prop.name + ": ")
        append(prop.call().toString())
        if (endWithNewLine) appendLine()
    }

    companion object {
        context(KtAnalysisSession)
        fun render(effect: KtEffectDeclaration, printer: PrettyPrinter, symbolRenderer: DebugSymbolRenderer) {
            effect.accept(KtDebugContractRenderer(this@KtAnalysisSession, symbolRenderer), Context(printer, endWithNewLine = false))
        }
    }
}
