/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtCallableMemberCall
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.KtMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

internal fun KtAnalysisSession.stringRepresentation(any: Any): String = with(any) {
    fun KtType.render() = asStringForDebugging().replace('/', '.')
    fun String.indented() = replace("\n", "\n  ")
    return when (this) {
        is KtFunctionLikeSymbol -> buildString {
            append(
                when (this@with) {
                    is KtFunctionSymbol -> callableIdIfNonLocal ?: name
                    is KtSamConstructorSymbol -> callableIdIfNonLocal ?: name
                    is KtConstructorSymbol -> "<constructor>"
                    is KtPropertyGetterSymbol -> callableIdIfNonLocal ?: "<getter>"
                    is KtPropertySetterSymbol -> callableIdIfNonLocal ?: "<setter>"
                    else -> error("unexpected symbol kind in KtCall: ${this@with::class.java}")
                }
            )
            append("(")
            (this@with as? KtFunctionSymbol)?.receiverType?.let { receiver ->
                append("<extension receiver>: ${receiver.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }

            @Suppress("DEPRECATION")
            (this@with as? KtCallableSymbol)?.getDispatchReceiverType()?.let { dispatchReceiverType ->
                append("<dispatch receiver>: ${dispatchReceiverType.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }
            valueParameters.joinTo(this) { stringRepresentation(it) }
            append(")")
            append(": ${returnType.render()}")
        }
        is KtValueParameterSymbol -> "${if (isVararg) "vararg " else ""}$name: ${returnType.render()}"
        is KtTypeParameterSymbol -> this.nameOrAnonymous.asString()
        is KtVariableSymbol -> "${if (isVal) "val" else "var"} $name: ${returnType.render()}"
        is KtSymbol -> DebugSymbolRenderer.render(this)
        is Boolean -> toString()
        is Map<*, *> -> if (isEmpty()) "{}" else entries.joinToString(
            separator = ",\n  ",
            prefix = "{\n  ",
            postfix = "\n}"
        ) { (k, v) -> "${k?.let { stringRepresentation(it).indented() }} -> (${v?.let { stringRepresentation(it).indented() }})" }
        is Collection<*> -> if (isEmpty()) "[]" else joinToString(
            separator = ",\n  ",
            prefix = "[\n  ",
            postfix = "\n]"
        ) {
            it?.let { stringRepresentation(it).indented() } ?: "null"
        }
        is PsiElement -> this.text
        is KtSubstitutor.Empty -> "<empty substitutor>"
        is KtMapBackedSubstitutor -> {
            val mappingText = getAsMap().entries
                .joinToString(prefix = "{", postfix = "}") { (k, v) -> stringRepresentation(k) + " = " + v.asStringForDebugging() }
            "<map substitutor: $mappingText>"
        }
        is KtSubstitutor -> "<complex substitutor>"
        is KtDiagnostic -> "$severity<$factoryName: $defaultMessage>"
        is KtType -> render()
        is Enum<*> -> name
        is Name -> asString()
        else -> buildString {
            val clazz = this@with::class
            val className = clazz.simpleName!!
            append(className)
            appendLine(":")
            clazz.memberProperties
                .filter { it.name != "token" && it.visibility == KVisibility.PUBLIC }
                .joinTo(this, separator = "\n  ", prefix = "  ") { property ->
                    val name = property.name

                    @Suppress("UNCHECKED_CAST")
                    val value = (property as KProperty1<Any, *>).get(this@with)?.let {
                        if (className == "KtErrorCallInfo" && name == "candidateCalls") {
                            (it as Collection<KtCall>).sortedWith { call1, call2 -> compareCalls(call1, call2) }
                        } else it
                    }
                    val valueAsString = value?.let { stringRepresentation(it).indented() }
                    "$name = $valueAsString"
                }
        }
    }
}

internal fun KtAnalysisSession.compareCalls(call1: KtCall, call2: KtCall): Int {
    // The order of candidate calls is non-deterministic. Sort by symbol string value.
    if (call1 !is KtCallableMemberCall<*, *> || call2 !is KtCallableMemberCall<*, *>) return 0
    return stringRepresentation(call1.partiallyAppliedSymbol).compareTo(stringRepresentation(call2.partiallyAppliedSymbol))
}