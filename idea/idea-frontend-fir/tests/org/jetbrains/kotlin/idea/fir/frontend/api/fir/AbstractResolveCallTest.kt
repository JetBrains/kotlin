/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.frontend.api.test.framework.AbstractHLApiSingleModuleTest
import org.jetbrains.kotlin.idea.fir.low.level.api.test.base.expressionMarkerProvider
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.calls.KtCall
import org.jetbrains.kotlin.idea.frontend.api.calls.KtDelegatedConstructorCallKind
import org.jetbrains.kotlin.idea.frontend.api.calls.KtErrorCallTarget
import org.jetbrains.kotlin.idea.frontend.api.calls.KtSuccessCallTarget
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter

abstract class AbstractResolveCallTest : AbstractHLApiSingleModuleTest() {
    override fun doTestByFileStructure(ktFiles: List<KtFile>, module: TestModule, testServices: TestServices) {
        val ktFile = ktFiles.first()
        val expression = testServices.expressionMarkerProvider.getSelectedElement(ktFile)

        val actual = executeOnPooledThreadInReadAction {
            analyse(ktFile) {
                resolveCall(expression)?.stringRepresentation()
            }
        } ?: "null"
        testServices.assertions.assertEqualsToFile(testDataFileSibling(".txt"), actual)
    }

    private fun KtAnalysisSession.resolveCall(element: PsiElement): KtCall? = when (element) {
        is KtCallElement -> element.resolveCall()
        is KtBinaryExpression -> element.resolveCall()
        is KtUnaryExpression -> element.resolveCall()
        is KtValueArgument -> resolveCall(element.getArgumentExpression()!!)
        else -> error("Selected should be either KtCallElement, KtBinaryExpression, or KtUnaryExpression, but was $element")
    }

}

private fun KtCall.stringRepresentation(): String {
    fun KtType.render() = asStringForDebugging().replace('/', '.')
    fun Any.stringValue(): String = when (this) {
        is KtFunctionLikeSymbol -> buildString {
            append(if (this@stringValue is KtFunctionSymbol) callableIdIfNonLocal ?: name else "<constructor>")
            append("(")
            (this@stringValue as? KtFunctionSymbol)?.receiverType?.let { receiver ->
                append("<receiver>: ${receiver.type.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }
            valueParameters.joinTo(this) { parameter ->
                "${parameter.name}: ${parameter.annotatedType.type.render()}"
            }
            append(")")
            append(": ${annotatedType.type.render()}")
        }
        is KtValueParameterSymbol -> "$name: ${annotatedType.type.render()}"
        is KtSuccessCallTarget -> symbol.stringValue()
        is KtErrorCallTarget -> "ERR<${this.diagnostic.defaultMessage}, [${candidates.joinToString { it.stringValue() }}]>"
        is Boolean -> toString()
        is Map<*, *> -> entries.joinToString(prefix = "{ ", postfix = " }") { (k, v) -> "${k?.stringValue()} -> (${v?.stringValue()})" }
        is KtExpression -> this.text
        is KtDelegatedConstructorCallKind -> toString()
        else -> error("unexpected parameter type ${this::class}")
    }

    val callInfoClass = this::class
    return buildString {
        append(callInfoClass.simpleName!!)
        append(":\n")
        val propertyByName = callInfoClass.memberProperties.associateBy(KProperty1<*, *>::name)
        callInfoClass.primaryConstructor!!.parameters.joinTo(this, separator = "\n") { parameter ->
            val value = propertyByName[parameter.name]!!.javaGetter!!(this@stringRepresentation)?.stringValue()
            "${parameter.name!!} = $value"
        }
    }
}

