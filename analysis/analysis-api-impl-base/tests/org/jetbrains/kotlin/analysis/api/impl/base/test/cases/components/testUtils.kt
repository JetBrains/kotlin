/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtCallableMemberCall
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.KtMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KtDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.declarations.modifiers.renderers.KtRendererKeywordFilter
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

@OptIn(KtAnalysisApiInternals::class)
internal fun KtAnalysisSession.stringRepresentation(any: Any): String = with(any) {
    fun KtType.render() = asStringForDebugging().replace('/', '.')
    return when (this) {
        is KtFunctionLikeSymbol -> buildString {
            append(
                when (this@with) {
                    is KtFunctionSymbol -> callableIdIfNonLocal ?: name
                    is KtSamConstructorSymbol -> callableIdIfNonLocal ?: name
                    is KtConstructorSymbol -> "<constructor>"
                    is KtPropertyGetterSymbol -> callableIdIfNonLocal ?: "<getter>"
                    is KtPropertySetterSymbol -> callableIdIfNonLocal ?: "<setter>"
                    else -> error("unexpected symbol kind in KtCall: ${this@with::class}")
                }
            )
            append("(")
            (this@with as? KtFunctionSymbol)?.receiverParameter?.let { receiver ->
                append("<extension receiver>: ${receiver.type.render()}")
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
        is KtSymbol -> DebugSymbolRenderer().render(this)
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
        is CallableId -> toString()
        is KtCallableSignature<*> -> this.stringRepresentation()
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

context(KtAnalysisSession)
private fun KtCallableSignature<*>.stringRepresentation(): String = buildString {
    when (this@stringRepresentation) {
        is KtFunctionLikeSignature<*> -> append(KtFunctionLikeSignature::class.simpleName)
        is KtVariableLikeSignature<*> -> append(KtVariableLikeSignature::class.simpleName)
    }
    appendLine(":")
    val memberProperties = listOfNotNull(
        KtVariableLikeSignature<*>::name.takeIf { this@stringRepresentation is KtVariableLikeSignature<*> },
        KtCallableSignature<*>::receiverType,
        KtCallableSignature<*>::returnType,
        KtCallableSignature<*>::symbol,
        KtFunctionLikeSignature<*>::valueParameters.takeIf { this@stringRepresentation is KtFunctionLikeSignature<*> },
        KtCallableSignature<*>::callableIdIfNonLocal
    )
    memberProperties.joinTo(this, separator = "\n  ", prefix = "  ") { property ->
        @Suppress("UNCHECKED_CAST")
        val value = (property as KProperty1<Any, *>).get(this@stringRepresentation)
        val valueAsString = value?.let { stringRepresentation(it).indented() }
        "${property.name} = $valueAsString"
    }
}

private fun String.indented() = replace("\n", "\n  ")

internal fun KtAnalysisSession.prettyPrintSignature(signature: KtCallableSignature<*>): String = prettyPrint {
    when (signature) {
        is KtFunctionLikeSignature -> {
            append("fun ")
            signature.receiverType?.let { append('.'); append(it.render(position = Variance.INVARIANT)) }
            append((signature.symbol as KtNamedSymbol).name.asString())
            printCollection(signature.valueParameters, prefix = "(", postfix = ")") { parameter ->
                append(parameter.name.asString())
                append(": ")
                append(parameter.returnType.render(position = Variance.INVARIANT))
            }
            append(": ")
            append(signature.returnType.render(position = Variance.INVARIANT))
        }
        is KtVariableLikeSignature -> {
            val symbol = signature.symbol
            if (symbol is KtVariableSymbol) {
                append(if (symbol.isVal) "val" else "var")
                append(" ")
            }
            signature.receiverType?.let { append('.'); append(it.render(position = Variance.INVARIANT)) }
            append((symbol as KtNamedSymbol).name.asString())
            append(": ")
            append(signature.returnType.render(position = Variance.INVARIANT))
        }
    }
}


internal fun KtAnalysisSession.compareCalls(call1: KtCall, call2: KtCall): Int {
    // The order of candidate calls is non-deterministic. Sort by symbol string value.
    if (call1 !is KtCallableMemberCall<*, *> || call2 !is KtCallableMemberCall<*, *>) return 0
    return stringRepresentation(call1.partiallyAppliedSymbol).compareTo(stringRepresentation(call2.partiallyAppliedSymbol))
}

context(KtAnalysisSession)
internal fun renderScopeWithParentDeclarations(scope: KtScope): String = prettyPrint {
    fun KtSymbol.qualifiedNameString() = when (this) {
        is KtConstructorSymbol -> "<constructor> ${containingClassIdIfNonLocal?.asString()}"
        is KtClassLikeSymbol -> classIdIfNonLocal!!.asString()
        is KtCallableSymbol -> callableIdIfNonLocal!!.toString()
        else -> error("unknown symbol $this")
    }

    val renderingOptions = KtDeclarationRendererForSource.WITH_SHORT_NAMES.with {
        modifiersRenderer = modifiersRenderer.with {
            keywordsRenderer = keywordsRenderer.with { keywordFilter = KtRendererKeywordFilter.NONE }
        }
    }

    printCollection(scope.getAllSymbols().toList(), separator = "\n\n") { symbol ->
        val containingDeclaration = symbol.getContainingSymbol() as KtClassLikeSymbol
        append(symbol.render(renderingOptions))
        append(" fromClass ")
        append(containingDeclaration.classIdIfNonLocal?.asString())
        if (symbol.typeParameters.isNotEmpty()) {
            appendLine()
            withIndent {
                printCollection(symbol.typeParameters, separator = "\n") { typeParameter ->
                    val containingDeclarationForTypeParameter = typeParameter.getContainingSymbol()
                    append(typeParameter.render(renderingOptions))
                    append(" from ")
                    append(containingDeclarationForTypeParameter?.qualifiedNameString())
                }
            }
        }

        if (symbol is KtFunctionLikeSymbol && symbol.valueParameters.isNotEmpty()) {
            appendLine()
            withIndent {
                printCollection(symbol.valueParameters, separator = "\n") { typeParameter ->
                    val containingDeclarationForValueParameter = typeParameter.getContainingSymbol()
                    append(typeParameter.render(renderingOptions))
                    append(" from ")
                    append(containingDeclarationForValueParameter?.qualifiedNameString())
                }
            }
        }
    }
}