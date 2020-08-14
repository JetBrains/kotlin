/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir

import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.addExternalTestFiles
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.frontend.api.CallInfo
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtParameterSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightTestCase
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaGetter


abstract class AbstractResolveCallTest : @Suppress("DEPRECATION") KotlinLightCodeInsightTestCase() {
    override fun getTestDataPath(): String = KotlinTestUtils.getHomeDirectory() + "/"

    protected fun doTest(path: String) {
        addExternalTestFiles(path)
        configureByFile(path)
        val elements = editor.caretModel.caretsAndSelections.map { selection ->
            getSingleSelectedElement(selection)
        }

        val actualText = executeOnPooledThreadInReadAction {
            val callInfos = analyze(file as KtFile) {
                elements.map { element ->
                    when (element) {
                        is KtCallExpression -> element.resolveCall()
                        is KtBinaryExpression -> element.resolveCall()
                        else -> error("Selected should be either KtCallExpression or KtBinaryExpression but was $element")
                    }
                }
            }

            if (callInfos.isEmpty()) {
                error("There are should be at least one call selected")
            }

            val textWithoutLatestComments = run {
                val rawText = File(path).readText()
                """(?m)^// CALL:\s.*$""".toRegex().replace(rawText, "").trimEnd()
            }
            buildString {
                append(textWithoutLatestComments)
                append("\n\n")
                callInfos.joinTo(this, separator = "\n") { info ->
                    "// CALL: ${info?.stringRepresentation()}"
                }
            }
        }
        KotlinTestUtils.assertEqualsToFile(File(path), actualText)
    }


    private fun getSingleSelectedElement(selection: CaretState): PsiElement {
        val selectionRange = selection.getTextRange()
        val elements = file.elementsInRange(selectionRange)
        if (elements.size != 1) {
            val selectionText = file.text.substring(selectionRange.startOffset, selectionRange.endOffset)
            error("Single element should be found for selection `$selectionText`, but $elements were found")
        }
        return elements.first()
    }

    private fun CaretState.getTextRange() = TextRange.create(
        editor.logicalPositionToOffset(selectionStart!!),
        editor.logicalPositionToOffset(selectionEnd!!)
    )
}

private fun CallInfo.stringRepresentation(): String {
    fun KtType.render() = asStringForDebugging().replace('/', '.')
    fun Any.stringValue(): String? = when (this) {
        is KtFunctionLikeSymbol -> buildString {
            append(if (this@stringValue is KtFunctionSymbol) callableIdIfNonLocal ?: name else "<constructor>")
            append("(")
            (this@stringValue as? KtFunctionSymbol)?.receiverType?.let { receiver ->
                append("<receiver>: ${receiver.render()}")
                if (valueParameters.isNotEmpty()) append(", ")
            }
            valueParameters.joinTo(this) { parameter ->
                "${parameter.name}: ${parameter.type.render()}"
            }
            append(")")
            append(": ${type.render()}")
        }
        is KtParameterSymbol -> "$name: ${type.render()}"
        is Boolean -> toString()
        else -> error("unexpected parameter type ${this::class}")
    }

    val callInfoClass = this::class
    return buildString {
        append(callInfoClass.simpleName!!)
        append(": ")
        val propertyByName = callInfoClass.memberProperties.associateBy(KProperty1<*, *>::name)
        callInfoClass.primaryConstructor!!.parameters.joinTo(this) { parameter ->
            val value = propertyByName[parameter.name]!!.javaGetter!!(this@stringRepresentation)?.stringValue()
            "${parameter.name!!} = $value"
        }
    }
}

