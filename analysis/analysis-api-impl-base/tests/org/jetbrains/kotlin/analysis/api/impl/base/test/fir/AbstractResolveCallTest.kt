/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.calls.KtCall
import org.jetbrains.kotlin.analysis.api.calls.KtDelegatedConstructorCallKind
import org.jetbrains.kotlin.analysis.api.calls.KtErrorCallTarget
import org.jetbrains.kotlin.analysis.api.calls.KtSuccessCallTarget
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.FrontendApiTestConfiguratorService
import org.jetbrains.kotlin.analysis.api.impl.barebone.test.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.api.impl.base.KtMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.impl.base.test.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtPossibleMemberSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter

abstract class AbstractResolveCallTest(configurator: FrontendApiTestConfiguratorService) : AbstractHLApiSingleModuleTest(configurator) {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.first()
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile)

        val actual = executeOnPooledThreadInReadAction {
            analyseForTest(expression) {
                resolveCall(expression)?.let { stringRepresentation(it) }
            }
        } ?: "null"
        testServices.assertions.assertEqualsToTestDataFileSibling(actual)
    }

    private fun KtAnalysisSession.resolveCall(element: PsiElement): KtCall? = when (element) {
        is KtValueArgument -> resolveCall(element.getArgumentExpression()!!)
        is KtDeclarationModifierList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.resolveCall()
        }
        is KtFileAnnotationList -> {
            val annotationEntry = element.annotationEntries.singleOrNull()
                ?: error("Only single annotation entry is supported for now")
            annotationEntry.resolveCall()
        }
        is KtSimpleNameExpression -> element.resolveAccessorCall()
        is KtElement -> element.resolveCallIfPossible()
        else -> error("Selected element type (${element::class.simpleName}) is not supported for resolveCall()")
    }

}

private fun KtAnalysisSession.stringRepresentation(call: KtCall): String {
    fun KtType.render() = call.substitutor.substituteOrSelf(this).asStringForDebugging().replace('/', '.')
    fun Any.stringValue(): String = when (this) {
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
            (this@stringValue as? KtPossibleMemberSymbol)?.getDispatchReceiverType()?.let { dispatchReceiverType ->
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
        is KtSuccessCallTarget -> symbol.stringValue()
        is KtErrorCallTarget -> "ERR<${this.diagnostic.defaultMessage}, [${candidates.joinToString { it.stringValue() }}]>"
        is Boolean -> toString()
        is Map<*, *> -> entries.joinToString(prefix = "{ ", postfix = " }") { (k, v) -> "${k?.stringValue()} -> (${v?.stringValue()})" }
        is KtExpression -> this.text
        is KtDelegatedConstructorCallKind -> toString()
        is KtSubstitutor.Empty -> "<empty substitutor>"
        is KtMapBackedSubstitutor -> {
            val mappingText = getAsMap().orEmpty().entries
                .joinToString(prefix = "{", postfix = "}") { (k, v) -> k.stringValue() + " = " + v.asStringForDebugging() }
            "<map substitutor: $mappingText>"
        }
        is KtSubstitutor -> "<complex substitutor>"
        else -> error("unexpected parameter type ${this::class}")
    }

    val callInfoClass = call::class
    return buildString {
        append(callInfoClass.simpleName!!)
        append(":\n")
        val propertyByName =
            callInfoClass.memberProperties.associateBy(KProperty1<*, *>::name)
        callInfoClass.primaryConstructor!!.parameters
            .filter { it.name != "token" }
            .joinTo(this, separator = "\n") { parameter ->
                val name = parameter.name!!.removePrefix("_")
                val value = propertyByName[name]!!.javaGetter!!(call)?.stringValue()
                "$name = $value"
            }
    }
}

