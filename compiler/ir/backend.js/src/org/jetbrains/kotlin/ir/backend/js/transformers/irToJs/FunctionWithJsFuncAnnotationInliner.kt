/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.js.backend.ast.*

class FunctionWithJsFuncAnnotationInliner(private val jsFuncCall: IrCall, private val context: JsGenerationContext) {
    private val function = getJsFunctionImplementation()
    private val replacements = collectReplacementsForCall()

    fun generateResultStatement(): List<JsStatement> {
        return function.body.statements
            .run {
                SimpleJsCodeInliner(replacements)
                    .apply { acceptList(this@run) }
                    .withTemporaryVariablesForExpressions(this)
            }
    }

    private fun getJsFunctionImplementation(): JsFunction =
        context.staticContext.backendContext.getJsCodeForFunction(jsFuncCall.symbol)?.deepCopy()
            ?: compilationException("JS function not found", jsFuncCall)

    private fun collectReplacementsForCall(): Map<JsName, JsExpression> {
        val translatedArguments = Array(jsFuncCall.valueArgumentsCount) {
            jsFuncCall.getValueArgument(it)!!.accept(IrElementToJsExpressionTransformer(), context)
        }
        return function.parameters
            .mapIndexed { i, param -> param.name to translatedArguments[i] }
            .toMap()
    }
}

private class SimpleJsCodeInliner(private val replacements: Map<JsName, JsExpression>): RecursiveJsVisitor() {
    private val temporaryNamesForExpressions = mutableMapOf<JsName, JsExpression>()

    fun withTemporaryVariablesForExpressions(statements: List<JsStatement>): List<JsStatement> {
        if (temporaryNamesForExpressions.isEmpty()) {
            return statements
        }

        val variableDeclarations = temporaryNamesForExpressions.map { JsVars(JsVars.JsVar(it.key, it.value)) }
        return variableDeclarations + statements
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        if (nameRef.qualifier != null) return
        nameRef.name = nameRef.name?.getReplacement() ?: return
    }

    private fun JsName.declareNewTemporaryFor(expression: JsExpression): JsName {
        return JsName(ident, true)
            .also { temporaryNamesForExpressions[it] = expression }
    }

    private fun JsName.getReplacement(): JsName? {
        val expression = replacements[this] ?: return null
        return when {
            expression is JsNameRef && expression.qualifier == null -> expression.name!!
            else -> declareNewTemporaryFor(expression)
        }
    }
}
