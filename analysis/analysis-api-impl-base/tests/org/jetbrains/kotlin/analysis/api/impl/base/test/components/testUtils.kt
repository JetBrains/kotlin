/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
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

internal fun KtAnalysisSession.stringRepresentation(call: KtCallInfo): String {
    fun Any.stringValue(): String {
        fun KtType.render() = asStringForDebugging().replace('/', '.')
        fun String.indented() = replace("\n", "\n  ")
        return when (this) {
            is KtFunctionLikeSymbol -> buildString {
                append(
                    when (this@stringValue) {
                        is KtFunctionSymbol -> callableIdIfNonLocal ?: name
                        is KtSamConstructorSymbol -> callableIdIfNonLocal ?: name
                        is KtConstructorSymbol -> "<constructor>"
                        is KtPropertyGetterSymbol -> callableIdIfNonLocal ?: "<getter>"
                        is KtPropertySetterSymbol -> callableIdIfNonLocal ?: "<setter>"
                        else -> error("unexpected symbol kind in KtCall: ${this@stringValue::class.java}")
                    }
                )
                append("(")
                (this@stringValue as? KtFunctionSymbol)?.receiverType?.let { receiver ->
                    append("<extension receiver>: ${receiver.render()}")
                    if (valueParameters.isNotEmpty()) append(", ")
                }

                @Suppress("DEPRECATION")
                (this@stringValue as? KtCallableSymbol)?.getDispatchReceiverType()?.let { dispatchReceiverType ->
                    append("<dispatch receiver>: ${dispatchReceiverType.render()}")
                    if (valueParameters.isNotEmpty()) append(", ")
                }
                valueParameters.joinTo(this) { it.stringValue() }
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
            ) { (k, v) -> "${k?.stringValue()?.indented()} -> (${v?.stringValue()?.indented()})" }
            is Collection<*> -> if (isEmpty()) "[]" else joinToString(
                separator = ",\n  ",
                prefix = "[\n  ",
                postfix = "\n]"
            ) {
                it?.stringValue()?.indented() ?: "null"
            }
            is PsiElement -> this.text
            is KtSubstitutor.Empty -> "<empty substitutor>"
            is KtMapBackedSubstitutor -> {
                val mappingText = getAsMap().entries
                    .joinToString(prefix = "{", postfix = "}") { (k, v) -> k.stringValue() + " = " + v.asStringForDebugging() }
                "<map substitutor: $mappingText>"
            }
            is KtSubstitutor -> "<complex substitutor>"
            is KtDiagnostic -> "$severity<$factoryName: $defaultMessage>"
            is KtType -> render()
            is Enum<*> -> name
            is Name -> asString()
            else -> buildString {
                val clazz = this@stringValue::class
                val className = clazz.simpleName!!
                append(className)
                appendLine(":")
                clazz.memberProperties
                    .filter { it.name != "token" && it.visibility == KVisibility.PUBLIC }
                    .joinTo(this, separator = "\n  ", prefix = "  ") { property ->
                        val name = property.name

                        @Suppress("UNCHECKED_CAST")
                        val value = (property as KProperty1<Any, *>).get(this@stringValue)?.let {
                            if (className == "KtErrorCallInfo" && name == "candidateCalls") {
                                // The order of calls in KtErrorCallInfo.candidateCalls is non-deterministic. Sort by symbol string value.
                                (it as Collection<KtCall>).sortedWith { call1, call2 ->
                                    if (call1 is KtCallableMemberCall<*, *> && call2 is KtCallableMemberCall<*, *>) {
                                        call1.partiallyAppliedSymbol.stringValue().compareTo(call2.partiallyAppliedSymbol.stringValue())
                                    } else 0
                                }
                            } else it
                        }
                        val valueAsString = value?.stringValue()?.indented()
                        "$name = $valueAsString"
                    }
            }
        }
    }

    return call.stringValue()
}