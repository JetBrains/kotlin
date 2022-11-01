/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.js.backend.ast.*

private typealias Replacement = Pair<JsExpression, IrValueParameter>

class FunctionWithJsFuncAnnotationInliner(private val jsFuncCall: IrCall, private val context: JsGenerationContext) {
    private val function = getJsFunctionImplementation()
    private val replacements = collectReplacementsForCall()

    fun generateResultStatement(): List<JsStatement> {
        val irFunction = jsFuncCall.symbol.owner
        val newContext = context.newFile(irFunction.file, irFunction, context.localNames)
        return function.body.statements
            .run {
                SimpleJsCodeInliner(replacements, newContext)
                    .apply { acceptList(this@run) }
                    .withTemporaryVariablesForExpressions(this)
            }
    }

    private fun getJsFunctionImplementation(): JsFunction =
        context.staticContext.backendContext.getJsCodeForFunction(jsFuncCall.symbol)?.deepCopy()
            ?: compilationException("JS function not found", jsFuncCall)

    private fun collectReplacementsForCall(): Map<JsName, Replacement> {
        val translatedArguments = Array(jsFuncCall.valueArgumentsCount) {
            jsFuncCall.getValueArgument(it)!!
                .accept(IrElementToJsExpressionTransformer(), context) to jsFuncCall.symbol.owner.valueParameters[it]
        }
        return function.parameters
            .mapIndexed { i, param -> param.name to translatedArguments[i] }
            .toMap()
    }
}

private class SimpleJsCodeInliner(private val replacements: Map<JsName, Replacement>, val context: JsGenerationContext) :
    RecursiveJsVisitor()
{
    private val temporaryNamesForExpressions = mutableMapOf<JsName, Replacement>()

    fun withTemporaryVariablesForExpressions(statements: List<JsStatement>): List<JsStatement> {
        if (temporaryNamesForExpressions.isEmpty()) {
            return statements
        }

        val variableDeclarations = temporaryNamesForExpressions.map {
            JsVars(JsVars.JsVar(it.key, it.value.first).withSource(it.value.second, context, useNameOf = it.value.second))
        }
        return variableDeclarations + statements
    }

    override fun visitNameRef(nameRef: JsNameRef) {
        super.visitNameRef(nameRef)
        if (nameRef.qualifier != null) return
        nameRef.name = nameRef.name?.getReplacement() ?: return
    }

    private fun JsName.declareNewTemporaryFor(expression: JsExpression, irValueParameter: IrValueParameter): JsName {
        return JsName(ident, true)
            .also { temporaryNamesForExpressions[it] = expression to irValueParameter }
    }

    private fun JsName.getReplacement(): JsName? {
        val (expression, irValueParameter) = replacements[this] ?: return null
        return when {
            expression is JsNameRef && expression.qualifier == null -> expression.name!!
            else -> declareNewTemporaryFor(expression, irValueParameter)
        }
    }
}
